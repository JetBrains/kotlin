/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.intellij.util.containers.ContainerUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateRangesCodegenTestData {
    private static final File TEST_DATA_DIR = new File("compiler/testData/codegen/boxWithStdlib/ranges");
    private static final File AS_LITERAL_DIR = new File(TEST_DATA_DIR, "literal");
    private static final File AS_EXPRESSION_DIR = new File(TEST_DATA_DIR, "expression");
    private static final File[] SOURCE_TEST_FILES = {
            new File("libraries/stdlib/test/language/RangeIterationTest.kt"),
            new File("libraries/stdlib/test/language/RangeIterationJVMTest.kt")
    };

    private static final Pattern TEST_FUN_PATTERN = Pattern.compile("test fun (\\w+)\\(\\) \\{.+?}", Pattern.DOTALL);
    private static final Pattern SUBTEST_INVOCATION_PATTERN = Pattern.compile("doTest\\(([^,]+), [^,]+, [^,]+, [^,]+,\\s+listOf[\\w<>]*\\(([^\\n]*)\\)\\)", Pattern.DOTALL);

    // $LIST.size() check is needed in order for tests not to run forever
    public static final String LITERAL_TEMPLATE =    "    val $LIST = ArrayList<$TYPE>()\n" +
                                                     "    for (i in $RANGE_EXPR) {\n" +
                                                     "        $LIST.add(i)\n" +
                                                     "        if ($LIST.size() > 23) break\n" +
                                                     "    }\n" +
                                                     "    if ($LIST != listOf<$TYPE>($LIST_ELEMENTS)) {\n" +
                                                     "        return \"Wrong elements for $RANGE_EXPR_ESCAPED: $$LIST\"\n" +
                                                     "    }\n" +
                                                     "\n";

    public static final String EXPRESSION_TEMPLATE = "    val $LIST = ArrayList<$TYPE>()\n" +
                                                     "    val $RANGE = $RANGE_EXPR\n" +
                                                     "    for (i in $RANGE) {\n" +
                                                     "        $LIST.add(i)\n" +
                                                     "        if ($LIST.size() > 23) break\n" +
                                                     "    }\n" +
                                                     "    if ($LIST != listOf<$TYPE>($LIST_ELEMENTS)) {\n" +
                                                     "        return \"Wrong elements for $RANGE_EXPR_ESCAPED: $$LIST\"\n" +
                                                     "    }\n" +
                                                     "\n";

    private static final Map<String, String> ELEMENT_TYPE_KNOWN_SUBSTRINGS = new ContainerUtil.ImmutableMapBuilder<String, String>()
            .put("'", "Char")
            .put("\"", "Char")
            .put("Float.NaN", "Float")
            .put("Double.NaN", "Double")
            .put("MaxL", "Long")
            .put("MinL", "Long")
            .put("MaxC", "Char")
            .put("MinC", "Char")
            .build();

    private static String detectElementType(String rangeExpression) {
        Matcher matcher = Pattern.compile("\\.to(\\w+)").matcher(rangeExpression);
        if (matcher.find()) {
            String elementType = matcher.group(1);
            return elementType.equals("Byte") || elementType.equals("Short") ? "Int" : elementType;
        }
        if (Pattern.compile("\\d\\.\\d").matcher(rangeExpression).find()) {
            return "Double";
        }
        for (String substring : ELEMENT_TYPE_KNOWN_SUBSTRINGS.keySet()) {
            if (rangeExpression.contains(substring)) {
                return ELEMENT_TYPE_KNOWN_SUBSTRINGS.get(substring);
            }
        }
        return "Int";
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

    private static final List<String> INTEGER_PRIMITIVES = Arrays.asList("Integer", "Byte", "Short", "Long", "Character");

    private static void writeToFile(File file, String generatedBody) {
        PrintWriter out;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            out = new PrintWriter(file);
        }
        catch (FileNotFoundException e) {
            throw new AssertionError(e);
        }

        out.println("// Auto-generated by " + GenerateRangesCodegenTestData.class.getName() + ". DO NOT EDIT!");
        out.println("import java.util.ArrayList");
        if (generatedBody.contains("Max") || generatedBody.contains("Min")) {
            // Import min/max values, but only in case when the generated test case actually uses them (not to clutter tests which don't)
            out.println();
            for (String primitive : INTEGER_PRIMITIVES) {
                out.println("import java.lang." + primitive + ".MAX_VALUE as Max" + primitive.charAt(0));
                out.println("import java.lang." + primitive + ".MIN_VALUE as Min" + primitive.charAt(0));
            }
        }
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
