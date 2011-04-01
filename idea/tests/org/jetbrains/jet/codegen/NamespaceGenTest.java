package org.jetbrains.jet.codegen;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLightProjectDescriptor;
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
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testPSVM() throws Exception {
        loadFile("PSVM.jet");
        final String text = generateToText();
        System.out.println(text);

        final Method main = generateFunction();
        Object[] args = new Object[] { new String[0] };
        main.invoke(null, args);
    }

    public void testReturnOne() throws Exception {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/returnOne.jet");
        final String text = generateToText();
        System.out.println(text);

        final Method main = generateFunction();
        final Object returnValue = main.invoke(null, new Object[0]);
        assertEquals(new Integer(42), returnValue);
    }

    public void testReturnA() throws Exception {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/returnA.jet");
        final String text = generateToText();
        System.out.println(text);

        final Method main = generateFunction();
        final Object returnValue = main.invoke(null, 50);
        assertEquals(new Integer(50), returnValue);
    }

    public void testLocalProperty() throws Exception {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/localProperty.jet");
        final String text = generateToText();
        System.out.println(text);

        final Method main = generateFunction();
        final Object returnValue = main.invoke(null, 76);
        assertEquals(new Integer(50), returnValue);
    }

    public void testCurrentTime() throws Exception {
        loadFile("currentTime.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        final long returnValue = (Long) main.invoke(null);
        long currentTime = System.currentTimeMillis();
        assertTrue(Math.abs(returnValue - currentTime) <= 1L);
    }

    public void testIdentityHashCode() throws Exception {
        loadFile("identityHashCode.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        Object o = new Object();
        final int returnValue = (Integer) main.invoke(null, o);
        assertEquals(returnValue, System.identityHashCode(o));
    }

    public void testSystemOut() throws Exception {
        loadFile("systemOut.jet");
        final Method main = generateFunction();
        final Object returnValue = main.invoke(null);
        assertEquals(returnValue, System.out);
    }

    public void testHelloWorld() throws Exception {
        loadFile("helloWorld.jet");

        System.out.println(generateToText());

        generateFunction();  // assert that it can be verified
    }

    public void testPlus() throws Exception {
        loadFile("plus.jet");

        System.out.println(generateToText());

        final Method main = generateFunction();
        final int returnValue = (Integer) main.invoke(null, 37, 5);
        assertEquals(42, returnValue);
    }

    public void testIf() throws Exception {
        loadFile("if.jet");

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(15, main.invoke(null, true));
        assertEquals(20, main.invoke(null, false));
    }

    public void testSingleBranchIf() throws Exception {
        loadFile("singleBranchIf.jet");

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(15, main.invoke(null, true));
        assertEquals(20, main.invoke(null, false));
    }

    public void testAssign() throws Exception {
        loadFile("assign.jet");

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(2, main.invoke(null));
    }

    public void testGt() throws Exception {
        loadFile("gt.jet");

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, 1));
        assertEquals(false, main.invoke(null, 0));
    }

    public void testWhile() throws Exception {
        factorialTest("while.jet");
    }

    public void testDoWhile() throws Exception {
        factorialTest("doWhile.jet");
    }

    public void testBreak() throws Exception {
        factorialTest("break.jet");
    }

    private void factorialTest(final String name) throws IllegalAccessException, InvocationTargetException {
        loadFile(name);

        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 3));
        assertEquals(120, main.invoke(null, 5));
    }

    private void loadFile(final String name) {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/" + name);
    }

    private String generateToText() {
        StringWriter writer = new StringWriter();
        JetFile jetFile = (JetFile) myFixture.getFile();
        JetNamespace namespace = jetFile.getRootNamespace();
        NamespaceCodegen codegen = new NamespaceCodegen(getProject(),
                new TraceClassVisitor(new PrintWriter(writer)),
                namespace.getFQName());
        codegen.generate(namespace);
        codegen.done();
        return writer.toString();
    }

    private Class generateToClass() {
        JetFile jetFile = (JetFile) myFixture.getFile();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        final JetNamespace namespace = jetFile.getRootNamespace();
        NamespaceCodegen codegen = new NamespaceCodegen(getProject(),
                writer,
                namespace.getFQName());
        codegen.generate(namespace);
        final byte[] data = writer.toByteArray();
        MyClassLoader classLoader = new MyClassLoader(NamespaceGenTest.class.getClassLoader());
        final Class aClass = classLoader.doDefineClass(NamespaceCodegen.getJVMClassName(namespace.getFQName()).replace("/", "."), data);
        return aClass;
    }

    private Method generateFunction() {
        Class aClass = generateToClass();
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

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }
}
