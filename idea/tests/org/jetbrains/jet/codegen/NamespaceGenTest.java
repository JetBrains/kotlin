package org.jetbrains.jet.codegen;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author yole
 */
public class NamespaceGenTest extends LightCodeInsightFixtureTestCase {
    public void testPSVM() throws InvocationTargetException, IllegalAccessException {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/PSVM.jet");
        JetFile jetFile = (JetFile) myFixture.getFile();
        JetNamespace namespace = jetFile.getRootNamespace();
        NamespaceCodegen codegen = new NamespaceCodegen();
        final String text = generateToText(namespace, codegen);
        System.out.println(text);

        final Class aClass = generateToClass(namespace, codegen);
        final Method[] methods = aClass.getMethods();
        final Method main = methods[0];
        Object[] args = new Object[] { new String[0] };
        main.invoke(null, args);
    }

    private static String generateToText(JetNamespace namespace, NamespaceCodegen codegen) {
        StringWriter writer = new StringWriter();
        codegen.generate(namespace, new TraceClassVisitor(new PrintWriter(writer)));
        return writer.toString();
    }

    private static Class generateToClass(JetNamespace namespace, NamespaceCodegen codegen) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        codegen.generate(namespace, writer);
        final byte[] data = writer.toByteArray();
        MyClassLoader classLoader = new MyClassLoader(NamespaceGenTest.class.getClassLoader());
        final Class aClass = classLoader.doDefineClass(NamespaceCodegen.getJVMClassName(namespace).replace("/", "."), data);
        return aClass;
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
