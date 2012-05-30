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

import com.google.common.io.Files;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.TestCaseWithTmpdir;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.plugin.JetLanguage;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test correctness of written local variables in class file for specified method
 *
 * @author Natalia.Ukhorskaya
 */

public class CheckLocalVariablesTableTest extends TestCaseWithTmpdir {

    private final File ktFile;
    private JetCoreEnvironment jetCoreEnvironment;

    public CheckLocalVariablesTableTest(File ktFile) {
        this.ktFile = ktFile;
    }

    @Override
    public String getName() {
        return ktFile.getName();
    }

    @Override
    protected void runTest() throws Throwable {
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable);

        String text = FileUtil.loadFile(ktFile);

        LightVirtualFile virtualFile = new LightVirtualFile(ktFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject()))
                .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
        assert psiFile != null;

        final ClassFileFactory factory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile, CompilerSpecialMode.REGULAR);

        String modifiedTestName = this.getName().replace(".kt", ".class");
        boolean isClassFound = false;
        for (String filename : factory.files()) {
            if (filename.equals(modifiedTestName)) {
                isClassFound = true;
                ClassReader cr = new ClassReader(factory.asBytes(filename));
                final List<LocalVariable> expectedLocalVariables = parseExpectations();
                final List<LocalVariable> actualLocalVariables = readLocalVariable(cr, parseMethodName());

                assertEquals("Count of variables are different", expectedLocalVariables.size(), actualLocalVariables.size());

                int index = 0;
                for (LocalVariable expectedVariable : expectedLocalVariables) {
                    LocalVariable actualVariable = actualLocalVariables.get(index);
                    assertEquals("Names are different", expectedVariable.name, actualVariable.name);
                    assertEquals("Types are different", expectedVariable.type, actualVariable.type);
                    assertEquals("Indexes are different", expectedVariable.index, actualVariable.index);
                    index++;
                }
            }
        }

        if (!isClassFound) {
            throw new AssertionError("file name should be the same as class name. File name is " + modifiedTestName);
        }


        Disposer.dispose(myTestRootDisposable);
    }


    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/checkLocalVariablesTable", true,
                                                    new JetTestCaseBuilder.NamedTestFactory() {
                                                        @NotNull
                                                        @Override
                                                        public Test createTest(@NotNull String dataPath,
                                                                @NotNull String name,
                                                                @NotNull File file) {
                                                            return new CheckLocalVariablesTableTest(file);
                                                        }
                                                    });
    }

    private static class LocalVariable {
        private final String name;
        private final String type;
        private final int index;

        private LocalVariable(
                @NotNull String name,
                @NotNull String type,
                int index
        ) {
            this.name = name;
            this.type = type;
            this.index = index;
        }
    }

    @NotNull
    private static final Pattern methodPattern = Pattern.compile("^// METHOD : *(.*)");

    private static final Pattern namePattern = Pattern.compile("^// VARIABLE : NAME=*(.*) TYPE=.* INDEX=.*");
    private static final Pattern typePattern = Pattern.compile("^// VARIABLE : NAME=.* TYPE=*(.*) INDEX=.*");
    private static final Pattern indexPattern = Pattern.compile("^// VARIABLE : NAME=.* TYPE=.* INDEX=*(.*)");


    private List<LocalVariable> parseExpectations() throws IOException {
        List<String> lines = Files.readLines(ktFile, Charset.forName("utf-8"));
        List<LocalVariable> expectedLocalVariables = new ArrayList<LocalVariable>();
        for (int i = lines.size() - 3; i < lines.size(); ++i) {
            Matcher nameMatcher = namePattern.matcher(lines.get(i));
            if (nameMatcher.matches()) {
                Matcher typeMatcher = typePattern.matcher(lines.get(i));
                Matcher indexMatcher = indexPattern.matcher(lines.get(i));

                if (!typeMatcher.matches()
                    || !indexMatcher.matches()) {
                    throw new AssertionError("Incorrect test instructions format");
                }


                expectedLocalVariables.add(new LocalVariable(nameMatcher.group(1),
                                                             typeMatcher.group(1),
                                                             Integer.parseInt(indexMatcher.group(1))));
            }
        }
        if (expectedLocalVariables.size() == 0) {
            throw new AssertionError("test instructions not found in " + ktFile);
        }
        else {
            return expectedLocalVariables;
        }
    }

    private String parseMethodName() throws IOException {
        List<String> lines = Files.readLines(ktFile, Charset.forName("utf-8"));
        for (int i = 0; i < lines.size(); ++i) {
            Matcher methodMatcher = methodPattern.matcher(lines.get(i));
            if (methodMatcher.matches()) {
                return methodMatcher.group(1);
            }
        }

        assertTrue("method instructions not found", false);
        return null;
    }

    @NotNull
    private List<LocalVariable> readLocalVariable(ClassReader cr, final String methodName) throws Exception {
        class Visitor extends EmptyVisitor {
            List<LocalVariable> readVariables = new ArrayList<LocalVariable>();

            @Override
            public MethodVisitor visitMethod(int access, String name, final String desc, final String signature, String[] exceptions) {
                if (methodName.equals(name + desc)) {
                    return new EmptyVisitor() {
                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                            readVariables.add(new LocalVariable(name, desc, index));
                        }

                    };
                }
                else {
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            }
        }
        Visitor visitor = new Visitor();

        cr.accept(visitor, ClassReader.SKIP_FRAMES);

        assertFalse("method not found: " + methodName, visitor.readVariables.size() == 0);

        return visitor.readVariables;
    }
}

