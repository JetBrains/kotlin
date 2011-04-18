package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Ref;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLightProjectDescriptor;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetFileType;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author yole
 */
public abstract class CodegenTestCase extends LightCodeInsightFixtureTestCase {
    protected void loadText(final String text) {
        myFixture.configureByText(JetFileType.INSTANCE, text);
    }

    protected void loadFile(final String name) {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/" + name);
    }

    protected String generateToText() {
        Codegens state = new Codegens(getProject(), true);
        JetFile jetFile = (JetFile) myFixture.getFile();
        JetNamespace namespace = jetFile.getRootNamespace();
        NamespaceCodegen codegen = state.forNamespace(namespace);
        codegen.generate(namespace);

        List<String> files = state.files();
        assertEquals("This test only supposed to generate single class file", 1, files.size());

        return state.asText(files.get(0));
    }

    private Class generateNamespaceClass() {
        Codegens state = new Codegens(getProject(), false);
        JetFile jetFile = (JetFile) myFixture.getFile();
        final JetNamespace namespace = jetFile.getRootNamespace();

        NamespaceCodegen codegen = state.forNamespace(namespace);
        codegen.generate(namespace);

        return getJVMClass(state, NamespaceCodegen.getJVMClassName(namespace.getFQName()).replace("/", "."));
    }

    protected Class generateClass() {
        final Codegens state = new Codegens(getProject(), false);
        JetFile jetFile = (JetFile) myFixture.getFile();
        final Ref<Class> result = new Ref<Class>();
        final ClassCodegen codegen = state.forClass(AnalyzingUtils.analyzeFile(jetFile, ErrorHandler.THROW_EXCEPTION));
        jetFile.acceptChildren(new JetVisitor() {
            @Override
            public void visitJetElement(JetElement elem) {
                elem.acceptChildren(this);
            }

            @Override
            public void visitClass(JetClass klass) {
                codegen.generate(klass);
                result.set(getJVMClass(state, klass.getName() + "$$Impl"));
            }
        });
        if (result.isNull()) {
            throw new IllegalArgumentException("found no classes in the file to generate");
        }
        return result.get();
    }

    private static Class getJVMClass(Codegens state, final String fqName) {
        List<String> files = state.files();
        assertEquals("This test only supposed to generate single class file", 1, files.size());
        final byte[] data = state.asBytes(files.get(0));
        MyClassLoader classLoader = new MyClassLoader(NamespaceGenTest.class.getClassLoader());
        return classLoader.doDefineClass(fqName, data);
    }

    protected Method generateFunction() {
        Class aClass = generateNamespaceClass();
        return aClass.getMethods()[0];
    }

    protected Method generateFunction(String name) {
        Class aClass = generateNamespaceClass();
        for (Method method : aClass.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException("couldn't find method " + name);
    }

    private static class MyClassLoader extends ClassLoader {
      public MyClassLoader(ClassLoader parent) {
        super(parent);
      }

      public Class doDefineClass(String name, byte[] data) {
        return defineClass(name, data, 0, data.length);
      }

      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
      }
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }
}
