/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.asm4.*;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author udalov
 */
public class LineNumberTest extends TestCaseWithTmpdir {

    private final String LINE_NUMBER_FUN = "lineNumber";
    private final Pattern TEST_LINE_NUMBER_PATTERN = Pattern.compile("^.*test." + LINE_NUMBER_FUN + "\\(\\).*$");

    private String getTestDataPath() {
        return JetParsingTest.getTestDataDir() + "/lineNumber";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable, ConfigurationKind.JDK_ONLY);
        File base = new File(getTestDataPath(), "base.kt");
        String text = FileUtil.loadFile(base);

        JetFile psiFile = JetTestUtils.createFile(base.getName(), text, jetCoreEnvironment.getProject());
        ClassFileFactory classFileFactory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);
        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, tmpdir);
    }

    private void doTest(File file) {
        JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(myTestRootDisposable, CompileCompilerDependenciesTest
                .compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), tmpdir));

        String text;
        try {
            text = FileUtil.loadFile(file);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }

        JetFile psiFile = JetTestUtils.createFile(file.getName(), text, jetCoreEnvironment.getProject());
        assert psiFile != null;

        List<Integer> expectedLineNumbers = extractSelectedLineNumbers(text);

        GenerationState state = GenerationUtils.compileFileGetGenerationStateForTest(psiFile);
        ClassFileFactory factory = state.getFactory();

        List<Integer> actualLineNumbers = Lists.newArrayList();
        for (String filename : factory.files()) {
            ClassReader cr = new ClassReader(factory.asBytes(filename));
            try {
                actualLineNumbers.addAll(readLineNumbers(cr));
            }
            catch (Throwable e) {
                System.out.println(state.getFactory().createText());
                throw ExceptionUtils.rethrow(e);
            }
        }
        assertSameElements(actualLineNumbers, expectedLineNumbers);
    }

    private void doTest() {
        doTest(new File(getTestDataPath() + "/" + getTestName(true) + ".kt"));
    }

    private List<Integer> extractSelectedLineNumbers(String fileContent) {
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

    private List<Integer> readLineNumbers(ClassReader cr) {
        final List<Label> labels = Lists.newArrayList();
        final Map<Label, Integer> labels2LineNumbers = Maps.newHashMap();

        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4) {
            @Override
            public MethodVisitor visitMethod(int access, String name, final String desc, final String signature, String[] exceptions) {
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
}
