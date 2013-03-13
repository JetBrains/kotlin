/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.*;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineNumberTest extends TestCaseWithTmpdir {

    private static final String LINE_NUMBER_FUN = "lineNumber";
    private static final Pattern TEST_LINE_NUMBER_PATTERN = Pattern.compile("^.*test." + LINE_NUMBER_FUN + "\\(\\).*$");

    @NotNull
    private String getTestDataPath() {
        return JetParsingTest.getTestDataDir() + "/lineNumber";
    }

    @NotNull
    private JetCoreEnvironment createEnvironment() {
        return new JetCoreEnvironment(myTestRootDisposable, JetTestUtils
                .compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), tmpdir));
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        JetCoreEnvironment environment = createEnvironment();
        JetFile psiFile = JetTestUtils.createFile(LINE_NUMBER_FUN + ".kt",
                                               "package test;\n\npublic fun " + LINE_NUMBER_FUN + "(): Int = 0\n",
                                               environment.getProject());

        ClassFileFactory classFileFactory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);
        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, tmpdir);
    }

    @NotNull
    private JetFile createPsiFile(@NotNull String filename) {
        File file = new File(getTestDataPath() + "/" + filename);
        JetCoreEnvironment environment = createEnvironment();

        String text;
        try {
            text = FileUtil.loadFile(file);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }

        return JetTestUtils.createFile(file.getName(), text, environment.getProject());
    }

    private void doTest(@NotNull String filename, boolean custom) {
        JetFile psiFile = createPsiFile(filename);
        GenerationState state = GenerationUtils.compileFileGetGenerationStateForTest(psiFile);

        List<Integer> expectedLineNumbers, actualLineNumbers;
        if (custom) {
            expectedLineNumbers = extractCustomLineNumbersFromSource(psiFile);
            actualLineNumbers = extractActualLineNumbersFromBytecode(state, false);
            assertEquals(expectedLineNumbers, actualLineNumbers);
        }
        else {
            expectedLineNumbers = extractSelectedLineNumbersFromSource(psiFile);
            actualLineNumbers = extractActualLineNumbersFromBytecode(state, true);
            assertSameElements(actualLineNumbers, expectedLineNumbers);
        }

    }

    @NotNull
    private List<Integer> extractActualLineNumbersFromBytecode(@NotNull GenerationState state, boolean testFunInvoke) {
        ClassFileFactory factory = state.getFactory();
        List<Integer> actualLineNumbers = Lists.newArrayList();
        for (String filename : factory.files()) {
            ClassReader cr = new ClassReader(factory.asBytes(filename));
            try {
                List<Integer> lineNumbers = testFunInvoke ? readTestFunLineNumbers(cr) : readAllLineNumbers(cr);
                actualLineNumbers.addAll(lineNumbers);
            }
            catch (Throwable e) {
                System.out.println(factory.createText());
                throw ExceptionUtils.rethrow(e);
            }
        }

        return actualLineNumbers;
    }

    private void doTest() {
        doTest(getTestName(true) + ".kt", false);
    }

    private void doTestCustom() {
        doTest("custom/" + getTestName(true) + ".kt", true);
    }

    @NotNull
    private List<Integer> extractCustomLineNumbersFromSource(@NotNull JetFile file) {
        String fileContent = file.getText();
        List<Integer> lineNumbers = Lists.newArrayList();
        String[] lines = StringUtil.convertLineSeparators(fileContent).split("\n");

        for (String line : lines) {
            if (line.startsWith("//")) {
                String[] numbers = line.substring("//".length()).trim().split(" +");
                for (String number : numbers) {
                    lineNumbers.add(Integer.parseInt(number));
                }
            }
        }

        return lineNumbers;
    }

    @NotNull
    private List<Integer> extractSelectedLineNumbersFromSource(@NotNull JetFile file) {
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
    private List<Integer> readTestFunLineNumbers(@NotNull ClassReader cr) {
        final List<Label> labels = Lists.newArrayList();
        final Map<Label, Integer> labels2LineNumbers = Maps.newHashMap();

        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM4) {
                    private Label lastLabel;

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        if (LINE_NUMBER_FUN.equals(name)) {
                            assert lastLabel != null : "A function call with no preceding label";
                            labels.add(lastLabel);
                        }
                        lastLabel = null;
                    }

                    @Override
                    public void visitLabel(Label label) {
                        lastLabel = label;
                    }

                    @Override
                    public void visitLineNumber(int line, Label start) {
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
    private List<Integer> readAllLineNumbers(@NotNull ClassReader reader) {
        final List<Integer> result = new ArrayList<Integer>();
        reader.accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM4) {
                    @Override
                    public void visitLineNumber(int line, Label label) {
                        result.add(line);
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);
        return result;
    }


    public void testAnonymousFunction() {
        doTest();
    }

    public void testClass() {
        doTest();
    }

    public void testClassObject() {
        doTest();
    }

    public void testDefaultParameter() {
        doTest();
    }

    public void testEnum() {
        doTest();
    }

    public void testFor() {
        doTest();
    }

    public void testIf() {
        doTest();
    }

    public void testLocalFunction() {
        doTest();
    }

    public void testObject() {
        doTest();
    }

    public void testPropertyAccessor() {
        doTest();
    }

    public void testPsvm() {
        doTest();
    }

    public void testTopLevel() {
        doTest();
    }

    public void testTrait() {
        doTest();
    }

    public void testTryCatch() {
        doTest();
    }

    public void testWhile() {
        doTest();
    }

    public void testCompileTimeConstant() {
        doTestCustom();
    }

    public void testIfThen() {
        doTestCustom();
    }

    public void testIfThenElse() {
        doTestCustom();
    }

    public void testTryCatchExpression() {
        doTestCustom();
    }

    public void testTryCatchFinally() {
        doTestCustom();
    }

    public void testTryFinally() {
        doTestCustom();
    }

    public void testWhen() {
        doTestCustom();
    }

    public void testWhenSubject() {
        doTestCustom();
    }

    public void testStaticDelegate() {
        JetFile foo = createPsiFile("staticDelegate/foo.kt");
        JetFile bar = createPsiFile("staticDelegate/bar.kt");
        GenerationState state = GenerationUtils.compileManyFilesGetGenerationStateForTest(foo.getProject(), Arrays.asList(foo, bar));
        ClassReader reader = new ClassReader(state.getFactory().asBytes(PackageClassUtils.getPackageClassName(FqName.ROOT) + ".class"));

        // There must be exactly one line number attribute for each static delegate in namespace.class, and it should point to the first
        // line. There are two static delegates in this test, hence the [1, 1]
        List<Integer> expectedLineNumbers = Arrays.asList(1, 1);
        List<Integer> actualLineNumbers = readAllLineNumbers(reader);

        assertSameElements(actualLineNumbers, expectedLineNumbers);
    }
}
