package org.jetbrains.jet.codegen;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLightProjectDescriptor;
import org.jetbrains.jet.lang.JetFileType;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

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
        loadText("fun f() : Int { return 42; }");
        final String text = generateToText();
        System.out.println(text);

        final Method main = generateFunction();
        final Object returnValue = main.invoke(null, new Object[0]);
        assertEquals(new Integer(42), returnValue);
    }

    public void testReturnA() throws Exception {
        loadText("fun foo(a : Int) = a");
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
        loadText("fun f() : Long { return System.currentTimeMillis(); }");
        System.out.println(generateToText());
        final Method main = generateFunction();
        final long returnValue = (Long) main.invoke(null);
        long currentTime = System.currentTimeMillis();
        assertTrue(Math.abs(returnValue - currentTime) <= 1L);
    }

    public void testIdentityHashCode() throws Exception {
        loadText("fun f(o: Any) : Int { return System.identityHashCode(o); }");
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

    public void testContinue() throws Exception {
        loadFile("continue.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(3, main.invoke(null, 4));
        assertEquals(7, main.invoke(null, 5));
    }

    public void testDiv() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a / b", 12, 3, 4);
    }

    public void testMod() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a % b", 14, 3, 2);
    }

    public void testNE() throws Exception {
        final String code = "fun foo(a: Int, b: Int): Int = if (a != b) 1 else 0";
        binOpTest(code, 5, 5, 0);
        binOpTest(code, 5, 3, 1);
    }

    public void testGE() throws Exception {
        final String code = "fun foo(a: Int, b: Int): Int = if (a >= b) 1 else 0";
        binOpTest(code, 5, 5, 1);
        binOpTest(code, 3, 5, 0);
    }

    public void testIfNoElse() throws Exception {
        loadFile("ifNoElse.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(5, main.invoke(null, 5, true));
        assertEquals(10, main.invoke(null, 5, false));
    }

    public void testCondJumpOnStack() throws Exception {
        loadText("fun foo(a: String): Int = if (Boolean.parseBoolean(a)) 5 else 10");
        final Method main = generateFunction();
        assertEquals(5, main.invoke(null, "true"));
        assertEquals(10, main.invoke(null, "false"));
    }

    public void testReturnCmp() throws Exception {
        loadText("fun foo(a: Int, b: Int): Boolean = a == b");
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, 1, 1));
        assertEquals(false, main.invoke(null, 1, 2));
    }

    public void _testBoxedInt() throws Exception {
        loadText("fun foo(a: Int?): Int = if (a != null) a else 239");
        final Method main = generateFunction();
        assertEquals(610, main.invoke(null, 610));
        assertEquals(239, main.invoke(null, new Object[]{null}));
    }

    public void testIntBoxed() throws Exception {
        loadText("fun foo(s: String): Int? = Integer.getInteger(s, 239)");
        final Method main = generateFunction();
        assertEquals(239, main.invoke(null, "no.such.system.property"));
    }

    public void testBoxConstant() throws Exception {
        loadText("fun foo(): Int? = 239");
        final Method main = generateFunction();
        assertEquals(239, main.invoke(null));
    }

    public void testBoxVariable() throws Exception {
        loadText("fun foo(): Int? { var x = 239; return x; }");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(239, main.invoke(null));
    }

    public void testLong() throws Exception {
        loadText("fun foo(a: Long, b: Long): Long = a + b");
        System.out.println(generateToText());
        final Method main = generateFunction();
        long arg = (long) Integer.MAX_VALUE;
        long expected = 2 * (long) Integer.MAX_VALUE;
        assertEquals(expected, main.invoke(null, arg, arg));
    }

    public void testLongCmp() throws Exception {
        loadText("fun foo(a: Long, b: Long): Long = if (a == b) 0xffffffff else 0xfffffffe");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(0xffffffffL, main.invoke(null, 1, 1));
        assertEquals(0xfffffffeL, main.invoke(null, 1, 0));
    }

    public void testShort() throws Exception {
        binOpTest("fun foo(a: Short, b: Short): Int = a + b",
                Short.valueOf((short) 32767), Short.valueOf((short) 32767), 65534);
    }

    public void testShortCmp() throws Exception {
        binOpTest("fun foo(a: Short, b: Short): Boolean = a == b",
                Short.valueOf((short) 32767), Short.valueOf((short) 32767), true);
    }

    public void testByte() throws Exception {
        binOpTest("fun foo(a: Byte, b: Byte): Int = a + b",
                Byte.valueOf((byte) 127), Byte.valueOf((byte) 127), 254);
    }

    public void testByteCmp() throws Exception {
        binOpTest("fun foo(a: Byte, b: Byte): Int = if (a == b) 1 else 0",
                Byte.valueOf((byte) 127), Byte.valueOf((byte) 127), 1);
    }

    public void testByteLess() throws Exception {
        binOpTest("fun foo(a: Byte, b: Byte): Boolean = a < b",
                Byte.valueOf((byte) 126), Byte.valueOf((byte) 127), true);
    }

    public void testBooleanConstant() throws Exception {
        loadText("fun foo(): Boolean = true");
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null));
    }

    public void testChar() throws Exception {
        binOpTest("fun foo(a: Char, b: Char): Int = a + b", 'A', (char) 3, (int) 'D');
    }

    public void testFloat() throws Exception {
        binOpTest("fun foo(a: Float, b: Float): Float = a + b", 1.0f, 2.0f, 3.0f);
    }

    public void testFloatCmp() throws Exception {
        binOpTest("fun foo(a: Float, b: Float): Boolean = a == b", 1.0f, 1.0f, true);
    }

    public void testDouble() throws Exception {
        binOpTest("fun foo(a: Double, b: Double): Double = a + b", 1.0, 2.0, 3.0);
    }

    public void testDoubleCmp() throws Exception {
        binOpTest("fun foo(a: Double, b: Double): Boolean = a == b", 1.0, 2.0, false);
    }

    public void testDoubleToJava() throws Exception {
        loadText("fun foo(d: Double): String? = Double.toString(d)");
        final Method main = generateFunction();
        assertEquals("1.0", main.invoke(null, 1.0));
    }

    public void testDoubleToInt() throws Exception {
        loadText("fun foo(a: Double): Int = a.int");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(1, main.invoke(null, 1.0));
    }

    public void testCastConstant() throws Exception {
        loadText("fun foo(): Double = 1.dbl");
        final Method main = generateFunction();
        assertEquals(1.0, main.invoke(null));
    }

    public void testCastOnStack() throws Exception {
        loadText("fun foo(): Double = System.currentTimeMillis().dbl");
        final Method main = generateFunction();
        double currentTimeMillis = (double) System.currentTimeMillis();
        double result = (Double) main.invoke(null);
        double delta = Math.abs(currentTimeMillis - result);
        assertTrue(delta <= 1.0);
    }

    public void testNeg() throws Exception {
        loadText("fun foo(a: Int): Int = -a");
        final Method main = generateFunction();
        assertEquals(-10, main.invoke(null, 10));
    }

    public void testPreIncrement() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; ++x; return x;");
        final Method main = generateFunction();
        assertEquals(11, main.invoke(null, 10));
    }

    public void testPreIncrementValue() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; return ++x;");
        final Method main = generateFunction();
        assertEquals(11, main.invoke(null, 10));
    }

    public void testPreDecrement() throws Exception {
        loadText("fun foo(a: Int): Int { return --a;");
        final Method main = generateFunction();
        assertEquals(9, main.invoke(null, 10));
    }

    public void testPreIncrementLong() throws Exception {
        loadText("fun foo(a: Long): Long = ++a");
        final Method main = generateFunction();
        assertEquals(11L, main.invoke(null, 10L));
    }

    public void testPreIncrementFloat() throws Exception {
        loadText("fun foo(a: Float): Float = ++a");
        final Method main = generateFunction();
        assertEquals(2.0f, main.invoke(null, 1.0f));
    }

    public void testPreIncrementDouble() throws Exception {
        loadText("fun foo(a: Double): Double = ++a");
        final Method main = generateFunction();
        assertEquals(2.0, main.invoke(null, 1.0));
    }

    public void testShl() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a shl b", 1, 3, 8);
    }

    public void testShr() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a shr b", 8, 3, 1);
    }

    public void testBitAnd() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a and b", 0x77, 0x1f, 0x17);
    }

    public void testBitOr() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a or b", 0x77, 0x1f, 0x7f);
    }

    public void testBitXor() throws Exception {
        binOpTest("fun foo(a: Int, b: Int): Int = a xor b", 0x70, 0x1f, 0x6f);
    }

    public void testBitInv() throws Exception {
        loadText("fun foo(a: Int): Int = a.inv()");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(0xffff0000, main.invoke(null, 0x0000ffff));
    }

    public void testMixedTypes() throws Exception {
        binOpTest("fun foo(a: Int, b: Long): Long = a + b", 1, 2L, 3L);
    }

    public void testMixedTypes2() throws Exception {
        binOpTest("fun foo(a: Double, b: Int): Double = a + b", 1.0, 2, 3.0);
    }

    public void testAugAssign() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; x += 5; return x; }");
        final Method main = generateFunction();
        assertEquals(10, main.invoke(null, 5));
    }

    public void testPostIncrementTypeInferenceFail() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; var y = x++; if (y+1 != x) return -1; return x; }");
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 5));
    }

    public void testPostIncrement() throws Exception {
        loadText("fun foo(a: Int): Int { var x = a; var y = x++; return x*y; }");
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 2));
    }

    public void testPostIncrementLong() throws Exception {
        loadText("fun foo(a: Long): Long { var x = a; var y = x++; return x*y; }");
        final Method main = generateFunction();
        assertEquals(6L, main.invoke(null, 2L));
    }

    public void testBooleanNot() throws Exception {
        loadText("fun foo(b: Boolean): Boolean = !b");
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, false));
        assertEquals(false, main.invoke(null, true));
    }

    public void testBooleanNotJump() throws Exception {
        loadText("fun foo(a: Int) : Int = if (!(a < 5)) a else 0");
        final Method main = generateFunction();
        assertEquals(6, main.invoke(null, 6));
        assertEquals(0, main.invoke(null, 4));
    }

    public void testAnd() throws Exception {
        loadText("fun foo(a : Int): Boolean = a > 0 && a/0 > 0");
        final Method main = generateFunction();
        assertEquals(false, main.invoke(null, 0));
        boolean hadException = false;
        try {
            main.invoke(null, 5);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof ArithmeticException) {
                hadException = true;
            }
        }
        assertTrue(hadException);
    }

    public void testOr() throws Exception {
        loadText("fun foo(a : Int): Boolean = a > 0 || a/0 > 0");
        final Method main = generateFunction();
        assertEquals(true, main.invoke(null, 5));
        boolean hadException = false;
        try {
            main.invoke(null, 0);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof ArithmeticException) {
                hadException = true;
            }
        }
        assertTrue(hadException);
    }

    public void testDecrementAsStatement() throws Exception {
        loadFile("bottles.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        main.invoke(null);  // ensure no exception
    }

    public void testBottles2() throws Exception {
        loadFile("bottles2.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        main.invoke(null);  // ensure no exception
    }

    public void testJavaConstructor() throws Exception {
        loadText("fun foo(): StringBuilder = new StringBuilder()");
        System.out.println(generateToText());
        final Method main = generateFunction();
        final Object result = main.invoke(null);
        assertTrue(result instanceof StringBuilder);
    }

    public void testJavaConstructorWithParameters() throws Exception {
        loadText("fun foo(): StringBuilder = new StringBuilder(\"beer\")");
        final Method main = generateFunction();
        final StringBuilder result = (StringBuilder) main.invoke(null);
        assertEquals("beer", result.toString());
    }

    public void testJavaEquals() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 == s2");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, new String("jet"), new String("jet")));
        assertEquals(Boolean.FALSE, main.invoke(null, new String("jet"), new String("ceylon")));
    }

    public void testJavaNotEquals() throws Exception {
        loadText("fun foo(s1: String, s2: String) = s1 != s2");
        final Method main = generateFunction();
        assertEquals(Boolean.FALSE, main.invoke(null, new String("jet"), new String("jet")));
        assertEquals(Boolean.TRUE, main.invoke(null, new String("jet"), new String("ceylon")));
    }

    public void testJavaEqualsNull() throws Exception {
        loadText("fun foo(s1: String?, s2: String?) = s1 == s2");
        final Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, null, null));
        assertEquals(Boolean.FALSE, main.invoke(null, "jet", null));
        assertEquals(Boolean.FALSE, main.invoke(null, null, "jet"));
    }

    public void testEqualsNullLiteral() throws Exception {
        loadText("fun foo(s: String?) = s == null");
        final Method main = generateFunction();
        assertEquals(Boolean.TRUE, main.invoke(null, new Object[] { null }));
        assertEquals(Boolean.FALSE, main.invoke(null, "jet"));
    }

    public void testFunctionCall() throws Exception {
        loadFile("functionCall.jet");
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals("foo", main.invoke(null));
    }

    private void binOpTest(final String text, final Object arg1, final Object arg2, final Object expected) throws Exception {
        loadText(text);
        System.out.println(generateToText());
        final Method main = generateFunction();
        assertEquals(expected, main.invoke(null, arg1, arg2));
    }

    private void loadText(final String text) {
        myFixture.configureByText(JetFileType.INSTANCE, text);
    }

    private void loadFile(final String name) {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/" + name);
    }

    private String generateToText() {
        Codegens state = new Codegens(getProject(), true);
        JetFile jetFile = (JetFile) myFixture.getFile();
        JetNamespace namespace = jetFile.getRootNamespace();
        NamespaceCodegen codegen = state.forNamespace(namespace);
        codegen.generate(namespace);

        List<String> files = state.files();
        assertEquals("This test only supposed to generate single class file", 1, files.size());

        return state.asText(files.get(0));
    }

    private Class generateToClass() {
        Codegens state = new Codegens(getProject(), false);
        JetFile jetFile = (JetFile) myFixture.getFile();
        final JetNamespace namespace = jetFile.getRootNamespace();

        NamespaceCodegen codegen = state.forNamespace(namespace);
        codegen.generate(namespace);

        List<String> files = state.files();
        assertEquals("This test only supposed to generate single class file", 1, files.size());
        final byte[] data = state.asBytes(files.get(0));
        MyClassLoader classLoader = new MyClassLoader(NamespaceGenTest.class.getClassLoader());
        return classLoader.doDefineClass(NamespaceCodegen.getJVMClassName(namespace.getFQName()).replace("/", "."), data);
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
