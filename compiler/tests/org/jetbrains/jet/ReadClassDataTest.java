package org.jetbrains.jet;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.codegen.PropertyCodegen;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.plugin.JetLanguage;
import org.junit.Assert;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Stepan Koltsov
 */
public class ReadClassDataTest extends UsefulTestCase {

    protected final Disposable myTestRootDisposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    private JetCoreEnvironment jetCoreEnvironment;
    private File tmpdir;
    
    private final File testFile;

    public ReadClassDataTest(@NotNull File testFile) {
        this.testFile = testFile;
        setName(testFile.getName());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tmpdir = JetTestUtils.tmpDirForTest(this);
        JetTestUtils.recreateDirectory(tmpdir);
    }

    @Override
    public void tearDown() throws Exception {
        Disposer.dispose(myTestRootDisposable);
    }

    private void createMockCoreEnvironment() {
        jetCoreEnvironment = new JetCoreEnvironment(myTestRootDisposable);

        final File rtJar = new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/rt.jar");
        jetCoreEnvironment.addToClasspath(rtJar);
        jetCoreEnvironment.addToClasspath(new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/annotations.jar"));
    }

    @Override
    public void runTest() throws Exception {
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable);

        String text = FileUtil.loadFile(testFile);

        LightVirtualFile virtualFile = new LightVirtualFile(testFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        GenerationState state = new GenerationState(jetCoreEnvironment.getProject(), ClassBuilderFactory.BINARIES);
        AnalyzingUtils.checkForSyntacticErrors(psiFile);
        BindingContext bindingContext = state.compile(psiFile);

        ClassFileFactory classFileFactory = state.getFactory();

        CompileEnvironment.writeToOutputDirectory(classFileFactory, tmpdir.getPath());
        
        NamespaceDescriptor namespaceFromSource = (NamespaceDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiFile);

        Assert.assertEquals("test", namespaceFromSource.getName());

        Disposer.dispose(myTestRootDisposable);


        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable);

        jetCoreEnvironment.addToClasspath(tmpdir);
        jetCoreEnvironment.addToClasspath(new File("out/production/stdlib"));

        JetSemanticServices jetSemanticServices = JetSemanticServices.createSemanticServices(jetCoreEnvironment.getProject());
        JavaSemanticServices semanticServices = new JavaSemanticServices(jetCoreEnvironment.getProject(), jetSemanticServices, new BindingTraceContext());

        JavaDescriptorResolver javaDescriptorResolver = semanticServices.getDescriptorResolver();
        NamespaceDescriptor namespaceFromClass = javaDescriptorResolver.resolveNamespace("test");
        
        compareNamespaces(namespaceFromSource, namespaceFromClass);
    }

    private void compareNamespaces(@NotNull NamespaceDescriptor nsa, @NotNull NamespaceDescriptor nsb) {
        Assert.assertEquals(nsa.getName(), nsb.getName());
        System.out.println("namespace " + nsa.getName());
        for (DeclarationDescriptor ad : nsa.getMemberScope().getAllDescriptors()) {
            if (ad instanceof ClassifierDescriptor) {
                ClassifierDescriptor bd = nsb.getMemberScope().getClassifier(ad.getName());
                compareClassifiers((ClassifierDescriptor) ad, bd);
                
                Assert.assertNull(nsb.getMemberScope().getClassifier(ad.getName() + JvmAbi.TRAIT_IMPL_SUFFIX));
            } else if (ad instanceof FunctionDescriptor) {
                Set<FunctionDescriptor> functions = nsb.getMemberScope().getFunctions(ad.getName());
                Assert.assertTrue(functions.size() >= 1);
                Assert.assertTrue("not implemented", functions.size() == 1);
                FunctionDescriptor bd = functions.iterator().next();
                compareDescriptors(ad, bd);
            } else if (ad instanceof PropertyDescriptor) {
                Set<VariableDescriptor> properties = nsb.getMemberScope().getProperties(ad.getName());
                Assert.assertTrue(properties.size() >= 1);
                Assert.assertTrue("not implemented", properties.size() == 1);
                PropertyDescriptor bd = (PropertyDescriptor) properties.iterator().next();
                compareDescriptors(ad, bd);

                Assert.assertTrue(nsb.getMemberScope().getFunctions(PropertyCodegen.getterName(ad.getName())).isEmpty());
                Assert.assertTrue(nsb.getMemberScope().getFunctions(PropertyCodegen.setterName(ad.getName())).isEmpty());
            } else {
                throw new AssertionError("Unknown member: " + ad);
            }
        }
    }

    private void compareClassifiers(@NotNull ClassifierDescriptor a, @NotNull ClassifierDescriptor b) {
        StringBuilder sba = new StringBuilder();
        StringBuilder sbb = new StringBuilder();
        
        new Serializer(sba).serializeContent((ClassDescriptor) a);
        new Serializer(sbb).serializeContent((ClassDescriptor) b);
        
        String as = sba.toString();
        String bs = sbb.toString();

        Assert.assertEquals(as, bs);
        System.out.println(as);
    }
    
    private void compareDescriptors(@NotNull DeclarationDescriptor a, @NotNull DeclarationDescriptor b) {
        StringBuilder sba = new StringBuilder();
        StringBuilder sbb = new StringBuilder();
        new Serializer(sba).serialize(a);
        new Serializer(sbb).serialize(b);
        
        String as = sba.toString();
        String bs = sbb.toString();

        Assert.assertEquals(as, bs);
        System.out.println(as);
    }




    private static class Serializer {

        protected final StringBuilder sb;

        public Serializer(StringBuilder sb) {
            this.sb = sb;
        }

        private String serializeContent(ClassDescriptor klass) {

            serialize(klass.getModality());
            sb.append(" ");

            serialize(klass.getKind());
            sb.append(" ");

            serialize(klass);

            if (!klass.getTypeConstructor().getParameters().isEmpty()) {
                sb.append("<");
                serializeCommaSeparated(klass.getTypeConstructor().getParameters());
                sb.append(">");
            }

            // TODO: supers
            // TODO: constructors

            sb.append(" {\n");

            List<TypeProjection> typeArguments = new ArrayList<TypeProjection>();
            for (TypeParameterDescriptor param : klass.getTypeConstructor().getParameters()) {
                typeArguments.add(new TypeProjection(Variance.INVARIANT, param.getDefaultType()));
            }

            JetScope memberScope = klass.getMemberScope(typeArguments);
            for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
                // TODO
                if (member.getName().equals("equals") || member.getName().equals("hashCode")
                        || member.getName().equals("wait") || member.getName().equals("notify") || member.getName().equals("notifyAll")
                        || member.getName().equals("toString") || member.getName().equals("getClass")
                        || member.getName().equals("clone") || member.getName().equals("finalize")
                        || member.getName().equals("getTypeInfo") || member.getName().equals("$setTypeInfo") || member.getName().equals("$typeInfo")
                        )
                {
                    continue;
                }
                sb.append("    ");
                new Serializer(sb).serialize(member);
                sb.append("\n");
            }

            sb.append("}\n");
            return sb.toString();
        }

        public void serialize(ClassKind kind) {
            switch (kind) {
                case CLASS:
                    sb.append("class");
                    break;
                case TRAIT:
                    sb.append("trait");
                    break;
                default:
                    throw new IllegalStateException();
            }
        }


        private static Object invoke(Method method, Object thiz, Object... args) {
            try {
                return method.invoke(thiz, args);
            } catch (Exception e) {
                throw new RuntimeException("failed to invoke " + method + ": " + e, e);
            }
        }


        public void serialize(FunctionDescriptor fun) {
            serialize(fun.getModality());
            sb.append(" ");

            sb.append("fun ");
            if (!fun.getTypeParameters().isEmpty()) {
                sb.append("<");
                serializeCommaSeparated(fun.getTypeParameters());
                sb.append(">");
            }

            if (fun.getReceiverParameter().exists()) {
                serialize(fun.getReceiverParameter());
                sb.append(".");
            }

            sb.append(fun.getName());
            sb.append("(");
            new ValueParameterSerializer(sb).serializeCommaSeparated(fun.getValueParameters());
            sb.append("): ");
            serialize(fun.getReturnType());
        }

        public void serialize(ExtensionReceiver extensionReceiver) {
            serialize(extensionReceiver.getType());
        }

        public void serialize(PropertyDescriptor prop) {
            if (prop.isVar()) {
                sb.append("var ");
            } else {
                sb.append("val ");
            }
            sb.append(prop.getName());
            sb.append(": ");
            serialize(prop.getOutType());
        }

        public void serialize(ValueParameterDescriptor valueParameter) {
            if (valueParameter.getVarargElementType() != null) {
                sb.append("vararg ");
            }
            sb.append(valueParameter.getName());
            sb.append(": ");
            if (valueParameter.getVarargElementType() != null) {
                serialize(valueParameter.getVarargElementType());
            } else {
                serialize(valueParameter.getOutType());
            }
            if (valueParameter.hasDefaultValue()) {
                sb.append(" = ?");
            }
        }

        public void serialize(Variance variance) {
            if (variance == Variance.INVARIANT) {

            } else {
                sb.append(variance);
                sb.append(' ');
            }
        }

        public void serialize(Modality modality) {
            sb.append(modality.name().toLowerCase());
        }

        public void serialize(JetType type) {
            serialize(type.getConstructor().getDeclarationDescriptor());
            if (!type.getArguments().isEmpty()) {
                sb.append("<");
                boolean first = true;
                for (TypeProjection proj : type.getArguments()) {
                    serialize(proj.getProjectionKind());
                    serialize(proj.getType());
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                }
                sb.append(">");
            }
        }

        public void serializeCommaSeparated(List<?> list) {
            serializeSeparated(list, ", ");
        }

        public void serializeSeparated(List<?> list, String sep) {
            boolean first = true;
            for (Object o : list) {
                if (!first) {
                    sb.append(sep);
                }
                serialize(o);
                first = false;
            }
        }

        private Method getMethodToSerialize(Object o) {
            // TODO: cache
            for (Method method : this.getClass().getMethods()) {
                if (!method.getName().equals("serialize")) {
                    continue;
                }
                if (method.getParameterTypes().length != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0].equals(Object.class)) {
                    continue;
                }
                if (method.getParameterTypes()[0].isInstance(o)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new IllegalStateException("don't know how to serialize " + o + " (of " + o.getClass() + ")");
        }

        public void serialize(Object o) {
            Method method = getMethodToSerialize(o);
            invoke(method, this, o);
        }

        public void serialize(String s) {
            sb.append(s);
        }

        public void serialize(ModuleDescriptor module) {
            // nop
        }

        public void serialize(ClassDescriptor clazz) {
            serialize(clazz.getContainingDeclaration());
            sb.append(".");
            sb.append(clazz.getName());
        }

        public void serialize(NamespaceDescriptor ns) {
            if (ns.getContainingDeclaration() == null) {
                // root ns
                return;
            }
            serialize(ns.getContainingDeclaration());
            sb.append(".");
            sb.append(ns.getName());
        }

        public void serialize(TypeParameterDescriptor param) {
            serialize(param.getVariance());
            sb.append(param.getName());
            if (!param.getUpperBounds().isEmpty()) {
                sb.append(" : ");
                List<String> list = new ArrayList<String>();
                for (JetType upper : param.getUpperBounds()) {
                    StringBuilder sb = new StringBuilder();
                    new ValueParameterSerializer(sb).serialize(upper);
                    list.add(sb.toString());
                }
                Collections.sort(list);
                serializeSeparated(list, " & "); // TODO: use where
            }
            // TODO: lower bounds
        }

    }
    
    private static class ValueParameterSerializer extends Serializer {

        public ValueParameterSerializer(StringBuilder sb) {
            super(sb);
        }

        @Override
        public void serialize(TypeParameterDescriptor param) {
            sb.append(param.getName());
        }
    }
    

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/readClass", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new ReadClassDataTest(file);
            }
        });
    }
    
}
