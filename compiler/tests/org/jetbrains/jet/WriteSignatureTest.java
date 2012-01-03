package org.jetbrains.jet;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.plugin.JetLanguage;
import org.junit.Assert;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.Method;

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
 * @author Stepan Koltsov
 *
 * @see CompileJavaAgainstKotlinTest
 */
public class WriteSignatureTest extends TestCaseWithTmpdir {

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
    protected void runTest() throws Throwable {
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable);


        String text = FileUtil.loadFile(ktFile);

        LightVirtualFile virtualFile = new LightVirtualFile(ktFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        GenerationState state = new GenerationState(jetCoreEnvironment.getProject(), ClassBuilderFactory.BINARIES);
        AnalyzingUtils.checkForSyntacticErrors(psiFile);
        state.compile(psiFile);

        ClassFileFactory classFileFactory = state.getFactory();

        CompileEnvironment.writeToOutputDirectory(classFileFactory, tmpdir.getPath());

        Disposer.dispose(myTestRootDisposable);

        final Expectation expectation = parseExpectations();

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
            class Visitor extends EmptyVisitor {
                ActualSignature readSignature;
                
                @Override
                public MethodVisitor visitMethod(int access, String name, final String desc, final String signature, String[] exceptions) {
                    if (name.equals(methodName)) {

                        final int parameterCount = new Method(name, desc).getArgumentTypes().length;

                        return new EmptyVisitor() {
                            String typeParameters = "";
                            String returnType;
                            String[] parameterTypes = new String[parameterCount];
                            
                            @Override
                            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                if (desc.equals(JvmStdlibNames.JET_METHOD.getDescriptor())) {
                                    return new EmptyVisitor() {
                                        @Override
                                        public void visit(String name, Object value) {
                                            if (name.equals(JvmStdlibNames.JET_METHOD_TYPE_PARAMETERS_FIELD)) {
                                                typeParameters = (String) value;
                                            } else if (name.equals(JvmStdlibNames.JET_METHOD_RETURN_TYPE_FIELD)) {
                                                returnType = (String) value;
                                            }
                                        }

                                        @Override
                                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                            return new EmptyVisitor();
                                        }

                                        @Override
                                        public AnnotationVisitor visitArray(String name) {
                                            return new EmptyVisitor();
                                        }
                                    };
                                } else {
                                    return new EmptyVisitor();
                                }
                            }

                            @Override
                            public AnnotationVisitor visitParameterAnnotation(final int parameter, String desc, boolean visible) {
                                if (desc.equals(JvmStdlibNames.JET_VALUE_PARAMETER.getDescriptor())) {
                                    return new EmptyVisitor() {
                                        @Override
                                        public void visit(String name, Object value) {
                                            if (name.equals(JvmStdlibNames.JET_VALUE_PARAMETER_TYPE_FIELD)) {
                                                parameterTypes[parameter] = (String) value;
                                            }
                                        }

                                        @Override
                                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                            return new EmptyVisitor();
                                        }

                                        @Override
                                        public AnnotationVisitor visitArray(String name) {
                                            return new EmptyVisitor();
                                        }
                                    };
                                } else {
                                    return new EmptyVisitor();
                                }
                            }

                            @Override
                            public AnnotationVisitor visitAnnotationDefault() {
                                return new EmptyVisitor();
                            }
                            
                            private String makeKotlinSignature() {
                                StringBuilder sb = new StringBuilder();
                                sb.append(typeParameters);
                                sb.append("(");
                                for (String parameterType : parameterTypes) {
                                    sb.append(parameterType);
                                }
                                sb.append(")");
                                sb.append(returnType);
                                return sb.toString();
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
            
            Assert.assertNotNull(visitor.readSignature);
            
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
