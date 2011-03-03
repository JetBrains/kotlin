package org.jetbrains.jet.codegen;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

/**
 * @author yole
 */
public class NamespaceGenTest extends LightCodeInsightFixtureTestCase {
    private NamespaceCodegen codegen;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        codegen = new NamespaceCodegen();
    }

    public void testPSVM() throws Exception {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/PSVM.jet");
        final String text = generateToText();
        System.out.println(text);

        final Class aClass = generateToClass();
        final Method main = firstMethod(aClass);
        Object[] args = new Object[] { new String[0] };
        main.invoke(null, args);
    }

    public void testReturnOne() throws Exception {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/returnOne.jet");
        final String text = generateToText();
        System.out.println(text);

        final Class aClass = generateToClass();
        final Method main = firstMethod(aClass);
        final Object returnValue = main.invoke(null, new Object[0]);
        assertEquals(new Integer(42), returnValue);
    }

    public void testReturnA() throws Exception {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/returnA.jet");
        final String text = generateToText();
        System.out.println(text);

        final Class aClass = generateToClass();
        final Method main = firstMethod(aClass);
        final Object returnValue = main.invoke(null, 42);
        assertEquals(new Integer(42), returnValue);
    }

    private String generateToText() {
        StringWriter writer = new StringWriter();
        JetFile jetFile = (JetFile) myFixture.getFile();
        JetNamespace namespace = jetFile.getRootNamespace();
        codegen.generate(namespace, new TraceClassVisitor(new PrintWriter(writer)), getProject());
        return writer.toString();
    }

    private Class generateToClass() {
        JetFile jetFile = (JetFile) myFixture.getFile();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        final JetNamespace namespace = jetFile.getRootNamespace();
        codegen.generate(namespace, writer, getProject());
        final byte[] data = writer.toByteArray();
        MyClassLoader classLoader = new MyClassLoader(NamespaceGenTest.class.getClassLoader());
        final Class aClass = classLoader.doDefineClass(NamespaceCodegen.getJVMClassName(namespace).replace("/", "."), data);
        return aClass;
    }

    private static Method firstMethod(Class aClass) {
        return aClass.getMethods()[0];
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
}
