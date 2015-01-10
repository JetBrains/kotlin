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

package org.jetbrains.jet.jvm.compiler;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test correctness of written JVM signature
 *
 * @see CompileJavaAgainstKotlinTestGenerated
 */
public abstract class AbstractWriteSignatureTest extends TestCaseWithTmpdir {

    private JetCoreEnvironment jetCoreEnvironment;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable);
    }

    @Override
    protected void tearDown() throws Exception {
        jetCoreEnvironment = null;
        super.tearDown();
    }

    protected void doTest(String ktFileName) throws Exception {
        File ktFile = new File(ktFileName);
        String text = FileUtil.loadFile(ktFile, true);

        JetFile psiFile = JetTestUtils.createFile(ktFile.getName(), text, jetCoreEnvironment.getProject());

        OutputFileCollection outputFiles = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        OutputUtilsPackage.writeAllTo(outputFiles, tmpdir);

        Disposer.dispose(myTestRootDisposable);

        Expectation expectation = parseExpectations(ktFile);

        ActualSignature actualSignature = readSignature(expectation.className, expectation.methodName);

        String template =
                "jvm signature:     %s\n" +
                "generic signature: %s\n" +
                "";

        String expected = String.format(template, expectation.jvmSignature, expectation.genericSignature);
        String actual = String.format(template, actualSignature.jvmSignature, actualSignature.genericSignature);

        Assert.assertEquals(expected, actual);
    }

    private static class ActualSignature {
        private final String jvmSignature;
        private final String genericSignature;

        private ActualSignature(@NotNull String jvmSignature, @Nullable String genericSignature) {
            this.jvmSignature = jvmSignature;
            this.genericSignature = genericSignature;
        }
    }

    @NotNull
    private ActualSignature readSignature(@NotNull String className, @Nullable final String methodName) throws Exception {
        // ugly unreadable code begin
        FileInputStream classInputStream = new FileInputStream(tmpdir + "/" + className.replace('.', '/') + ".class");
        try {
            class Visitor extends ClassVisitor {
                ActualSignature readSignature;

                public Visitor() {
                    super(Opcodes.ASM5);
                }

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    if (methodName == null) {
                        Assert.assertNull(readSignature);
                        readSignature = new ActualSignature(name, signature);
                    }
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, final String desc, final String signature, String[] exceptions) {
                    if (name.equals(methodName)) {

                        return new MethodVisitor(Opcodes.ASM5) {
                            @Override
                            public void visitEnd() {
                                Assert.assertNull(readSignature);
                                readSignature = new ActualSignature(desc, signature);
                            }
                        };
                    }
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            }

            Visitor visitor = new Visitor();

            new ClassReader(classInputStream).accept(visitor,
                                                     ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            Assert.assertNotNull("method not found: " + className + "::" + methodName, visitor.readSignature);

            return visitor.readSignature;
        }
        finally {
            Closeables.closeQuietly(classInputStream);
        }
        // ugly unreadable code end
    }

    private static class Expectation {
        private final String className;
        @Nullable
        private final String methodName;
        private final String jvmSignature;
        private final String genericSignature;

        private Expectation(
                @NotNull String className, @Nullable String methodName,
                @NotNull String jvmSignature, @Nullable String genericSignature
        ) {
            this.className = className;
            this.methodName = methodName;
            this.jvmSignature = jvmSignature;
            this.genericSignature = genericSignature;
        }
    }

    @NotNull
    private static final Pattern classPattern = Pattern.compile("^// class: *(.*)");
    private static final Pattern methodPattern = Pattern.compile("^// method: *(.*)::(.*?) *(//.*)?");
    private static final Pattern jvmSignaturePattern = Pattern.compile("^// jvm signature: *(.+?) *(//.*)?");
    private static final Pattern genericSignaturePattern = Pattern.compile("^// generic signature: *(.+?) *(//.*)?");

    private Expectation parseExpectations(File ktFile) throws IOException {
        List<String> lines = Files.readLines(ktFile, Charset.forName("utf-8"));
        for (int i = 0; i < lines.size() - 2; ++i) {
            String line = lines.get(i);
            Matcher methodMatcher = methodPattern.matcher(line);
            Matcher classMatcher = classPattern.matcher(line);
            boolean isMethod = methodMatcher.matches();
            boolean isClass = classMatcher.matches();
            Matcher matcher = isMethod ? methodMatcher : classMatcher;
            if (isMethod || isClass) {
                Matcher jvmSignatureMatcher = jvmSignaturePattern.matcher(lines.get(i + 1));
                Matcher genericSignatureMatcher = genericSignaturePattern.matcher(lines.get(i + 2));
                if (!jvmSignatureMatcher.matches() || !genericSignatureMatcher.matches()) {
                    throw new AssertionError("'method:' must be followed ... bla bla ... use the source luke");
                }

                String className = matcher.group(1);
                String methodName = isMethod ? matcher.group(2) : null;

                String jvmSignature = jvmSignatureMatcher.group(1);
                String genericSignature = genericSignatureMatcher.group(1);
                if (genericSignature.equals("null")) {
                    genericSignature = null;
                }
                return new Expectation(className, methodName, jvmSignature, genericSignature);
            }
        }
        throw new AssertionError("test instructions not found in " + ktFile);
    }
}
