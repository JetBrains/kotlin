package org.jetbrains.jet.codegen;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLightProjectDescriptor;
import org.jetbrains.jet.lang.JetFileType;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
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

        StringBuilder answer = new StringBuilder();

        List<String> files = state.files();
        for (String file : files) {
            answer.append("@").append(file).append('\n');
            answer.append(state.asText(file));
        }

        return answer.toString();
    }

    private Class generateNamespaceClass() {
        JetFile jetFile = (JetFile) myFixture.getFile();
        final JetNamespace namespace = jetFile.getRootNamespace();
        String fqName = NamespaceCodegen.getJVMClassName(namespace.getFQName()).replace("/", ".");
        Codegens state = generateClassesInFile();
        return loadClass(fqName, state);
    }

    protected Class loadClass(String fqName, Codegens state) {
        List<String> files = state.files();
        for (String file : files) {
            if (file.equals(fqName.replace('.', '/') + ".class")) {
                final byte[] data = state.asBytes(file);
                MyClassLoader classLoader = new MyClassLoader(NamespaceGenTest.class.getClassLoader());
                return classLoader.doDefineClass(fqName, data);
            }
        }

        fail("No classfile was generated for: " + fqName);
        return null;
    }

    protected Codegens generateClassesInFile() {
        Codegens state = new Codegens(getProject(), false);
        JetFile jetFile = (JetFile) myFixture.getFile();
        final JetNamespace namespace = jetFile.getRootNamespace();

        NamespaceCodegen codegen = state.forNamespace(namespace);
        codegen.generate(namespace);
        return state;
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
