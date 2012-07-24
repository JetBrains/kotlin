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
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.Opcodes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test correctness of written access flags in class file
 *
 * @author Natalia.Ukhorskaya
 */
public class WriteAccessFlagsTest extends TestCaseWithTmpdir {

    private final File ktFile;
    private JetCoreEnvironment jetCoreEnvironment;

    public WriteAccessFlagsTest(File ktFile) {
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

        final ClassFileFactory factory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        String modifiedTestName = this.getName().replace(".kt", ".class");
        boolean isClassFound = false;
        for (String filename : factory.files()) {
            if (filename.equals(modifiedTestName)) {
                isClassFound = true;
                ClassReader cr = new ClassReader(factory.asBytes(filename));
                final Expectation expectation = parseExpectations();
                int expectedAccess = 0;
                if (expectation.isAbstract) {
                    expectedAccess |= Opcodes.ACC_ABSTRACT;
                }
                if (expectation.isAnnotation) {
                    expectedAccess |= Opcodes.ACC_ANNOTATION;
                }
                if (expectation.isFinal) {
                    expectedAccess |= Opcodes.ACC_FINAL;
                }
                if (expectation.isPublic) {
                    expectedAccess |= Opcodes.ACC_PUBLIC;
                }
                if (expectation.isInterface) {
                    expectedAccess |= Opcodes.ACC_INTERFACE;
                }
                if (expectation.isStatic) {
                    expectedAccess |= Opcodes.ACC_STATIC;
                }
                if (expectation.isSuper) {
                    expectedAccess |= Opcodes.ACC_SUPER;
                }

                assertEquals("Wrong access flag", expectedAccess, cr.getAccess());
            }
        }

        if (!isClassFound) {
            throw new AssertionError("file name should be the same as class name. File name is " + modifiedTestName);
        }

        Disposer.dispose(myTestRootDisposable);
    }


    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/writeAccessFlags", true,
            new JetTestCaseBuilder.NamedTestFactory() {
                @NotNull
                @Override
                public Test createTest(@NotNull String dataPath,
                        @NotNull String name,
                        @NotNull File file) {
                    return new WriteAccessFlagsTest(file);
                }
            });
    }

    private static class Expectation {
        private final boolean isAbstract;
        private final boolean isAnnotation;
        private final boolean isFinal;
        private final boolean isInterface;
        private final boolean isPublic;
        private final boolean isStatic;
        private final boolean isSuper;

        private Expectation(@NotNull String isAbstract,
                @NotNull String isAnnotation,
                @NotNull String isFinal,
                @NotNull String isInterface,
                @NotNull String isPublic,
                @NotNull String isStatic,
                @NotNull String isSuper) {
            this.isAbstract = isAbstract.equals("true");
            this.isAnnotation = isAnnotation.equals("true");
            this.isFinal = isFinal.equals("true");
            this.isPublic = isPublic.equals("true");
            this.isInterface = isInterface.equals("true");
            this.isStatic = isStatic.equals("true");
            this.isSuper = isSuper.equals("true");
        }
    }

    @NotNull
    private static final Pattern isAbstract = Pattern.compile("^// ACC_ABSTRACT : *(.*)");
    private static final Pattern isAnnotation = Pattern.compile("^// ACC_ANNOTATION : *(.*)");
    private static final Pattern isFinal = Pattern.compile("^// ACC_FINAL : *(.*)");
    private static final Pattern isInterface = Pattern.compile("^// ACC_INTERFACE : *(.*)");
    private static final Pattern isPublic = Pattern.compile("^// ACC_PUBLIC : *(.*)");
    private static final Pattern isStatic = Pattern.compile("^// ACC_STATIC : *(.*)");
    private static final Pattern isSuper = Pattern.compile("^// ACC_SUPER : *(.*)");

    private Expectation parseExpectations() throws IOException {
        List<String> lines = Files.readLines(ktFile, Charset.forName("utf-8"));
        for (int i = 0; i < lines.size() - 6; ++i) {
            Matcher abstractMatcher = isAbstract.matcher(lines.get(i));
            if (abstractMatcher.matches()) {
                Matcher annotationMatcher = isAnnotation.matcher(lines.get(i + 1));
                Matcher finalMatcher = isFinal.matcher(lines.get(i + 2));
                Matcher interfaceMatcher = isInterface.matcher(lines.get(i + 3));
                Matcher publicMatcher = isPublic.matcher(lines.get(i + 4));
                Matcher staticMatcher = isStatic.matcher(lines.get(i + 5));
                Matcher superMatcher = isSuper.matcher(lines.get(i + 6));

                if (!interfaceMatcher.matches()
                    || !annotationMatcher.matches()
                    || !finalMatcher.matches()
                    || !interfaceMatcher.matches()
                    || !publicMatcher.matches()
                    || !staticMatcher.matches()
                    || !superMatcher.matches()) {
                    throw new AssertionError("Incorrect test instructions format");
                }

                return new Expectation(abstractMatcher.group(1),
                                       annotationMatcher.group(1),
                                       finalMatcher.group(1),
                                       interfaceMatcher.group(1),
                                       publicMatcher.group(1),
                                       staticMatcher.group(1),
                                       superMatcher.group(1));
            }
        }
        throw new AssertionError("test instructions not found in " + ktFile);
    }
}
