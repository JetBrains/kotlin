/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.generators.tests;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.LineSeparator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateRangesCodegenTestData {
    private static final File TEST_DATA_DIR = new File("compiler/testData/codegen/box/ranges");
    private static final File AS_LITERAL_DIR = new File(TEST_DATA_DIR, "literal");
    private static final File AS_EXPRESSION_DIR = new File(TEST_DATA_DIR, "expression");
    private static final File[] SOURCE_TEST_FILES = {
            new File("libraries/stdlib/test/ranges/RangeIterationTest.kt"),
    };

    private static final Pattern TEST_FUN_PATTERN = Pattern.compile("@Test fun (\\w+)\\(\\) \\{.+?}", Pattern.DOTALL);
    private static final Pattern SUBTEST_INVOCATION_PATTERN = Pattern.compile("doTest\\(([^,]+), [^,]+, [^,]+, [^,]+,\\s+listOf[\\w<>]*\\(([^\\n]*)\\)\\)", Pattern.DOTALL);

    // $LIST.size() check is needed in order for tests not to run forever
    private static final String LITERAL_TEMPLATE = "    val $LIST = ArrayList<$TYPE>()\n" +
                                                   "    for (i in $RANGE_EXPR) {\n" +
                                                   "        $LIST.add(i)\n" +
                                                   "        if ($LIST.size > 23) break\n" +
                                                   "    }\n" +
                                                   "    if ($LIST != listOf<$TYPE>($LIST_ELEMENTS)) {\n" +
                                                   "        return \"Wrong elements for $RANGE_EXPR_ESCAPED: $$LIST\"\n" +
                                                   "    }\n" +
                                                   "\n";

    private static final String EXPRESSION_TEMPLATE = "    val $LIST = ArrayList<$TYPE>()\n" +
                                                      "    val $RANGE = $RANGE_EXPR\n" +
                                                      "    for (i in $RANGE) {\n" +
                                                      "        $LIST.add(i)\n" +
                                                      "        if ($LIST.size > 23) break\n" +
                                                      "    }\n" +
                                                      "    if ($LIST != listOf<$TYPE>($LIST_ELEMENTS)) {\n" +
                                                      "        return \"Wrong elements for $RANGE_EXPR_ESCAPED: $$LIST\"\n" +
                                                      "    }\n" +
                                                      "\n";

    private static final List<String> INTEGER_PRIMITIVES = Arrays.asList("Int", "Byte", "Short", "Long", "Char", "UInt", "UByte", "UShort", "ULong");

    private static final Map<String, String> ELEMENT_TYPE_KNOWN_SUBSTRINGS = new HashMap<>();
    private static final Map<String, String> MIN_MAX_CONSTANTS = new LinkedHashMap<>();

    static {
        for (String integerType : INTEGER_PRIMITIVES) {
            String suffix = integerType.substring(0, integerType.startsWith("U") ? 2 : 1);
            ELEMENT_TYPE_KNOWN_SUBSTRINGS.put("Max" + suffix, integerType);
            ELEMENT_TYPE_KNOWN_SUBSTRINGS.put("Min" + suffix, integerType);
            MIN_MAX_CONSTANTS.put("Max" + suffix, integerType + ".MAX_VALUE");
            MIN_MAX_CONSTANTS.put("Min" + suffix, integerType + ".MIN_VALUE");
        }

        ELEMENT_TYPE_KNOWN_SUBSTRINGS.put("'", "Char");
        ELEMENT_TYPE_KNOWN_SUBSTRINGS.put("\"", "Char");
    }

    private static String detectElementType(String rangeExpression) {
        Matcher matcher = Pattern.compile("\\.to(\\w+)").matcher(rangeExpression);
        if (matcher.find()) {
            String elementType = matcher.group(1);
            return getResultingType(elementType);
        }
        for (String substring : ELEMENT_TYPE_KNOWN_SUBSTRINGS.keySet()) {
            if (rangeExpression.contains(substring)) {
                return getResultingType(ELEMENT_TYPE_KNOWN_SUBSTRINGS.get(substring));
            }
        }
        if (Pattern.compile("\\duL", Pattern.CASE_INSENSITIVE).matcher(rangeExpression).find()) {
            return "ULong";
        }
        if (Pattern.compile("\\dL").matcher(rangeExpression).find()) {
            return "Long";
        }
        if (Pattern.compile("\\du", Pattern.CASE_INSENSITIVE).matcher(rangeExpression).find()) {
            return "UInt";
        }
        return "Int";
    }

    private static String getResultingType(String operandType) {
        return operandType.equals("Byte") || operandType.equals("Short") ? "Int" :
               operandType.equals("UByte") || operandType.equals("UShort") ? "UInt" :
               operandType;
    }

    private static String renderTemplate(String template, int number, String elementType, String rangeExpression, String expectedListElements) {
        return template
                .replace("$RANGE_EXPR_ESCAPED", StringUtil.escapeStringCharacters(rangeExpression))
                .replace("$RANGE_EXPR", rangeExpression)
                .replace("$LIST_ELEMENTS", expectedListElements)
                .replace("$LIST", "list" + number)
                .replace("$RANGE", "range" + number)
                .replace("$TYPE", elementType)
                .replace("\n", LineSeparator.getSystemLineSeparator().getSeparatorString());
    }

    private static final List<String> IGNORED_FOR_JS_BACKEND = Collections.emptyList();

    private static final List<String> IGNORED_FOR_NATIVE_BACKEND = Collections.emptyList();

    private static void writeIgnoreBackendDirective(PrintWriter out, String backendName) {
        out.printf("// TODO: muted automatically, investigate should it be ran for %s or not%n", backendName);
        out.printf("// IGNORE_BACKEND: %s%n%n", backendName);
    }

    private static void writeToFile(File file, String generatedBody) {
        PrintWriter out;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            out = new PrintWriter(file);
        }
        catch (FileNotFoundException e) {
            throw new AssertionError(e);
        }

        // Ranges are not supported in JS_IR, JVM_IR yet
        writeIgnoreBackendDirective(out, "JS_IR");
        writeIgnoreBackendDirective(out, "JVM_IR");

        if (IGNORED_FOR_JS_BACKEND.contains(file.getName())) {
            writeIgnoreBackendDirective(out, "JS");
        }
        if (IGNORED_FOR_NATIVE_BACKEND.contains(file.getName())) {
            writeIgnoreBackendDirective(out, "NATIVE");
        }

        out.println("// Auto-generated by " + GenerateRangesCodegenTestData.class.getName() + ". DO NOT EDIT!");
        out.println("// WITH_RUNTIME");
        out.println();
        // Import min/max values, but only in case when the generated test case actually uses them (not to clutter tests which don't)
        out.println();
        MIN_MAX_CONSTANTS.forEach((name, value) -> {
            if (generatedBody.contains(name)) {
                out.printf("const val %s = %s", name, value).println();
            }
        });

        out.println();
        out.println("fun box(): String {");
        out.print(generatedBody);
        out.println("    return \"OK\"");
        out.println("}");
        out.close();
    }

    public static void main(String[] args) {
        try {
            FileUtil.delete(AS_LITERAL_DIR);
            FileUtil.delete(AS_EXPRESSION_DIR);
            //noinspection ResultOfMethodCallIgnored
            AS_LITERAL_DIR.mkdirs();
            //noinspection ResultOfMethodCallIgnored
            AS_EXPRESSION_DIR.mkdirs();

            for (File file : SOURCE_TEST_FILES) {
                String sourceContent = FileUtil.loadFile(file);
                Matcher testFunMatcher = TEST_FUN_PATTERN.matcher(sourceContent);
                while (testFunMatcher.find()) {
                    String testFunName = testFunMatcher.group(1);
                    if (testFunName.equals("emptyConstant")) {
                        continue;
                    }

                    String testFunText = testFunMatcher.group();

                    StringBuilder asLiteralBody = new StringBuilder();
                    StringBuilder asExpressionBody = new StringBuilder();
                    int index = 0;

                    Matcher matcher = SUBTEST_INVOCATION_PATTERN.matcher(testFunText);
                    while (matcher.find()) {
                        index++;
                        String rangeExpression = matcher.group(1);
                        String expectedListElements = matcher.group(2);
                        String elementType = detectElementType(rangeExpression);
                        asLiteralBody.append(renderTemplate(LITERAL_TEMPLATE, index, elementType, rangeExpression, expectedListElements));
                        asExpressionBody.append(renderTemplate(EXPRESSION_TEMPLATE, index, elementType, rangeExpression, expectedListElements));
                    }

                    String fileName = testFunName + ".kt";
                    writeToFile(new File(AS_LITERAL_DIR, fileName), asLiteralBody.toString());
                    writeToFile(new File(AS_EXPRESSION_DIR, fileName), asExpressionBody.toString());
                }
            }

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GenerateRangesCodegenTestData() {
    }
}
