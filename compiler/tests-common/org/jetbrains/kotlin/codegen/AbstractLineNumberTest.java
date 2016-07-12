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

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractLineNumberTest extends TestCaseWithTmpdir {
    private static final String LINE_NUMBER_FUN = "lineNumber";
    private static final Pattern TEST_LINE_NUMBER_PATTERN = Pattern.compile("^.*test." + LINE_NUMBER_FUN + "\\(\\).*$");

    @NotNull
    private KotlinCoreEnvironment createEnvironment() {
        return KotlinCoreEnvironment.createForTests(
                myTestRootDisposable,
                KotlinTestUtils.compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK,
                                                              KotlinTestUtils.getAnnotationsJar(), tmpdir),
                EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        KotlinCoreEnvironment environment = createEnvironment();
        KtFile psiFile = KotlinTestUtils.createFile(
                LINE_NUMBER_FUN + ".kt",
                "package test;\n\npublic fun " + LINE_NUMBER_FUN + "(): Int = 0\n",
                environment.getProject()
        );

        GenerationUtils.compileFileTo(psiFile, environment, tmpdir);
    }

    @NotNull
    private Pair<KtFile, KotlinCoreEnvironment> createPsiFile(@NotNull String filename) {
        File file = new File(filename);
        KotlinCoreEnvironment environment = createEnvironment();

        String text;
        try {
            text = FileUtil.loadFile(file, true);
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }

        return new Pair<KtFile, KotlinCoreEnvironment>(
                KotlinTestUtils.createFile(file.getName(), text, environment.getProject()), environment
        );
    }

    private void doTest(@NotNull String filename, boolean custom) {
        Pair<KtFile, KotlinCoreEnvironment> fileAndEnv = createPsiFile(filename);
        KtFile psiFile = fileAndEnv.getFirst();
        KotlinCoreEnvironment environment = fileAndEnv.getSecond();

        ClassFileFactory classFileFactory = GenerationUtils.compileFile(psiFile, environment);

        if (custom) {
            List<Integer> actualLineNumbers = extractActualLineNumbersFromBytecode(classFileFactory, false);
            String text = psiFile.getText();
            String newFileText = text.substring(0, text.indexOf("// ")) + getActualLineNumbersAsString(actualLineNumbers);
            KotlinTestUtils.assertEqualsToFile(new File(filename), newFileText);
        }
        else {
            List<Integer> expectedLineNumbers = extractSelectedLineNumbersFromSource(psiFile);
            List<Integer> actualLineNumbers = extractActualLineNumbersFromBytecode(classFileFactory, true);
            assertSameElements(actualLineNumbers, expectedLineNumbers);
        }

    }

    private static String getActualLineNumbersAsString(List<Integer> list) {
        return CollectionsKt.joinToString(list, " ", "// ", "", -1, "...", new Function1<Integer, CharSequence>() {
            @Override
            public CharSequence invoke(Integer integer) {
                return integer.toString();
            }
        });
    }

    @NotNull
    private static List<Integer> extractActualLineNumbersFromBytecode(@NotNull ClassFileFactory factory, boolean testFunInvoke) {
        List<Integer> actualLineNumbers = Lists.newArrayList();
        for (OutputFile outputFile : ClassFileUtilsKt.getClassFiles(factory)) {
            ClassReader cr = new ClassReader(outputFile.asByteArray());
            try {
                List<Integer> lineNumbers = testFunInvoke ? readTestFunLineNumbers(cr) : readAllLineNumbers(cr);
                actualLineNumbers.addAll(lineNumbers);
            }
            catch (Throwable e) {
                System.out.println(factory.createText());
                throw ExceptionUtilsKt.rethrow(e);
            }
        }

        return actualLineNumbers;
    }

    protected void doTest(String path) {
        doTest(path, false);
    }

    protected void doTestCustom(String path) {
        doTest(path, true);
    }

    @NotNull
    private static List<Integer> extractSelectedLineNumbersFromSource(@NotNull KtFile file) {
        String fileContent = file.getText();
        List<Integer> lineNumbers = Lists.newArrayList();
        String[] lines = StringUtil.convertLineSeparators(fileContent).split("\n");

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = TEST_LINE_NUMBER_PATTERN.matcher(lines[i]);
            if (matcher.matches()) {
                lineNumbers.add(i + 1);
            }
        }

        return lineNumbers;
    }

    @NotNull
    private static List<Integer> readTestFunLineNumbers(@NotNull ClassReader cr) {
        final List<Label> labels = Lists.newArrayList();
        final Map<Label, Integer> labels2LineNumbers = Maps.newHashMap();

        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    private Label lastLabel;

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (LINE_NUMBER_FUN.equals(name)) {
                            assert lastLabel != null : "A function call with no preceding label";
                            labels.add(lastLabel);
                        }
                        lastLabel = null;
                    }

                    @Override
                    public void visitLabel(@NotNull Label label) {
                        lastLabel = label;
                    }

                    @Override
                    public void visitLineNumber(int line, @NotNull Label start) {
                        labels2LineNumbers.put(start, line);
                    }
                };
            }
        };

        cr.accept(visitor, ClassReader.SKIP_FRAMES);

        List<Integer> lineNumbers = Lists.newArrayList();
        for (Label label : labels) {
            Integer lineNumber = labels2LineNumbers.get(label);
            assert lineNumber != null : "No line number found for a label";
            lineNumbers.add(lineNumber);
        }

        return lineNumbers;
    }

    @NotNull
    private static List<Integer> readAllLineNumbers(@NotNull ClassReader reader) {
        final List<Integer> result = new ArrayList<Integer>();
        reader.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitLineNumber(int line, @NotNull Label label) {
                        result.add(line);
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);
        return result;
    }
}
