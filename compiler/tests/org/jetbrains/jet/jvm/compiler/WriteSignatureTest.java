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
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.*;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
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
public class WriteSignatureTest extends TestCaseWithTmpdir {

    private static final AnnotationVisitor EMPTY_ANNOTATION_VISITOR = new AnnotationVisitor(Opcodes.ASM4) {};

    private final File ktFile;
    private JetCoreEnvironment jetCoreEnvironment;

    public WriteSignatureTest(File ktFile) {
        this.ktFile = ktFile;
    }

    @Override
    public String getName() {
        return ktFile.getName();
    }

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

    @Override
    protected void runTest() throws Throwable {
        String text = FileUtil.loadFile(ktFile);

        JetFile psiFile = JetTestUtils.createFile(ktFile.getName(), text, jetCoreEnvironment.getProject());

        ClassFileFactory classFileFactory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, tmpdir);

        Disposer.dispose(myTestRootDisposable);

        Expectation expectation = parseExpectations();

        ActualSignature actualSignature = readSignature(expectation.className, expectation.methodName);

        String template =
                "jvm signature:     %s\n" +
                "generic signature: %s\n" +
                "kotlin signature:  %s\n" +
                "";

        String expected = String.format(template, expectation.jvmSignature, expectation.genericSignature, expectation.kotlinSignature);
        String actual = String.format(template, actualSignature.jvmSignature, actualSignature.genericSignature, actualSignature.kotlinSignature);

        Assert.assertEquals(expected, actual);
    }
    
    private static class ActualSignature {
        private final String jvmSignature;
        private final String genericSignature;
        private final String kotlinSignature;

        private ActualSignature(@NotNull String jvmSignature, @Nullable String genericSignature, @Nullable String kotlinSignature) {
            this.jvmSignature = jvmSignature;
            this.genericSignature = genericSignature;
            this.kotlinSignature = kotlinSignature;
        }
    }
    
    @NotNull
    private ActualSignature readSignature(String className, final String methodName) throws Exception {
        // ugly unreadable code begin
        FileInputStream classInputStream = new FileInputStream(tmpdir + "/" + className.replace('.', '/') + ".class");
        try {
            class Visitor extends ClassVisitor {
                ActualSignature readSignature;

                public Visitor() {
                    super(Opcodes.ASM4);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, final String desc, final String signature, String[] exceptions) {
                    if (name.equals(methodName)) {

                        final int parameterCount = new Method(name, desc).getArgumentTypes().length;

                        return new MethodVisitor(Opcodes.ASM4) {
                            String typeParameters = "";
                            String returnType;
                            String[] parameterTypes = new String[parameterCount];
                            
                            @Override
                            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                if (desc.equals(JvmStdlibNames.JET_METHOD.getDescriptor())) {
                                    return new AnnotationVisitor(Opcodes.ASM4) {
                                        @Override
                                        public void visit(String name, Object value) {
                                            if (name.equals(JvmStdlibNames.JET_METHOD_TYPE_PARAMETERS_FIELD)) {
                                                typeParameters = (String) value;
                                            }
                                            else if (name.equals(JvmStdlibNames.JET_METHOD_RETURN_TYPE_FIELD)) {
                                                returnType = (String) value;
                                            }
                                        }

                                        @Override
                                        public AnnotationVisitor visitAnnotation(String s, String s1) {
                                            return EMPTY_ANNOTATION_VISITOR;
                                        }

                                        @Override
                                        public AnnotationVisitor visitArray(String name) {
                                            return EMPTY_ANNOTATION_VISITOR;
                                        }
                                    };
                                }
                                else {
                                    return EMPTY_ANNOTATION_VISITOR;
                                }
                            }

                            @Override
                            public AnnotationVisitor visitParameterAnnotation(final int parameter, String desc, boolean visible) {
                                if (desc.equals(JvmStdlibNames.JET_VALUE_PARAMETER.getDescriptor())) {
                                    return new AnnotationVisitor(Opcodes.ASM4) {
                                        @Override
                                        public void visit(String name, Object value) {
                                            if (name.equals(JvmStdlibNames.JET_VALUE_PARAMETER_TYPE_FIELD)) {
                                                parameterTypes[parameter] = (String) value;
                                            }
                                        }

                                        @Override
                                        public AnnotationVisitor visitAnnotation(String name, String desc) {
                                            return EMPTY_ANNOTATION_VISITOR;
                                        }

                                        @Override
                                        public AnnotationVisitor visitArray(String name) {
                                            return EMPTY_ANNOTATION_VISITOR;
                                        }
                                    };
                                }
                                else {
                                    return EMPTY_ANNOTATION_VISITOR;
                                }
                            }

                            @Override
                            public AnnotationVisitor visitAnnotationDefault() {
                                return EMPTY_ANNOTATION_VISITOR;
                            }

                            @Nullable
                            private String makeKotlinSignature() {
                                boolean allNulls = true;
                                
                                StringBuilder sb = new StringBuilder();
                                sb.append(typeParameters);
                                if (typeParameters != null && typeParameters.length() > 0) {
                                    allNulls = false;
                                }
                                sb.append("(");
                                for (String parameterType : parameterTypes) {
                                    sb.append(parameterType);
                                    if (parameterType != null) {
                                        allNulls = false;
                                    }
                                }
                                sb.append(")");
                                sb.append(returnType);
                                if (returnType != null) {
                                    allNulls = false;
                                }
                                if (allNulls) {
                                    return null;
                                }
                                else {
                                    return sb.toString();
                                }
                            }

                            @Override
                            public void visitEnd() {
                                Assert.assertNull(readSignature);
                                readSignature = new ActualSignature(desc, signature, makeKotlinSignature());
                            }
                        };
                    }
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            }
            
            Visitor visitor = new Visitor();
            
            new ClassReader(classInputStream).accept(visitor,
                    ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
            
            Assert.assertNotNull("method not found: " + className + "::" + methodName, visitor.readSignature);

            return visitor.readSignature;
        } finally {
            Closeables.closeQuietly(classInputStream);
        }
        // ugly unreadable code end
    }
    
    private static class Expectation {
        private final String className;
        private final String methodName;
        private final String jvmSignature;
        private final String genericSignature;
        private final String kotlinSignature;

        private Expectation(@NotNull String className, @NotNull String methodName,
                @NotNull String jvmSignature, @Nullable String genericSignature, @Nullable String kotlinSignature) {
            this.className = className;
            this.methodName = methodName;
            this.jvmSignature = jvmSignature;
            this.genericSignature = genericSignature;
            this.kotlinSignature = kotlinSignature;
        }
    }

    @NotNull
    private static final Pattern methodPattern = Pattern.compile("^// method: *(.*)::(.*?) *(//.*)?");
    private static final Pattern jvmSignaturePattern = Pattern.compile("^// jvm signature: *(.+?) *(//.*)?");
    private static final Pattern genericSignaturePattern = Pattern.compile("^// generic signature: *(.+?) *(//.*)?");
    private static final Pattern kotlinSignaturePattern = Pattern.compile("^// kotlin signature: *(.+?) *(//.*)?");
    private Expectation parseExpectations() throws IOException {
        List<String> lines = Files.readLines(ktFile, Charset.forName("utf-8"));
        for (int i = 0; i < lines.size() - 3; ++i) {
            Matcher methodMatcher = methodPattern.matcher(lines.get(i));
            if (methodMatcher.matches()) {
                Matcher jvmSignatureMatcher = jvmSignaturePattern.matcher(lines.get(i + 1));
                Matcher genericSignatureMatcher = genericSignaturePattern.matcher(lines.get(i + 2));
                Matcher kotlinSignatureMatcher = kotlinSignaturePattern.matcher(lines.get(i + 3));
                if (!jvmSignatureMatcher.matches() || !genericSignatureMatcher.matches() || !kotlinSignatureMatcher.matches()) {
                    throw new AssertionError("'method:' must be followed ... bla bla ... use the source luke");
                }
                
                String className = methodMatcher.group(1);
                String methodName = methodMatcher.group(2);
                
                String jvmSignature = jvmSignatureMatcher.group(1);
                String genericSignature = genericSignatureMatcher.group(1);
                String kotlinSignature = kotlinSignatureMatcher.group(1);
                if (genericSignature.equals("null")) {
                    genericSignature = null;
                }
                if (kotlinSignature.equals("null")) {
                    kotlinSignature = null;
                }
                return new Expectation(className, methodName, jvmSignature, genericSignature, kotlinSignature);
            }
        }
        throw new AssertionError("test instructions not found in " + ktFile);
    }

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/writeSignature", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new WriteSignatureTest(file);
            }
        });

    }

}
