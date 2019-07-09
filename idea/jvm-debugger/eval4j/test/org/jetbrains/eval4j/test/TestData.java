/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.eval4j.test;

class TestData extends BaseTestData {
   static void returnVoid() {
    }

    static boolean returnBoolean() {
        return true;
    }

    static byte returnByte() {
        return 2;
    }

    static short returnShort() {
        return 2;
    }

    static char returnChar() {
        return '2';
    }

    static int returnInt() {
        return 2;
    }

    static long returnLong() {
        return 2;
    }

    static float returnFloat() {
        return 2.0f;
    }

    static double returnDouble() {
        return 2.0d;
    }

    static Object returnNull() {
        return null;
    }

    static String returnString() {
        return "str";
    }

    static Object returnStringAsObject() {
        return "str";
    }

    static void checkCastNull() {
        CheckCastToNull klass = new CheckCastToNull();
        klass.f1(null);
        klass.f2(null);
        klass.f3(null);

        Integer integer = (Integer) null;
        Object object = (Object) null;
    }

    static class CheckCastToNull {
        void f1(Integer p) {}
        void f2(Integer[] p) {}
        void f3(Integer[][] p) {}
    }

    static Integer integerValueOf() { return 1; }

    static Byte byteValueOf() { return 1; }

    static Short shortValueOf() { return 1; }

    static Long longValueOf() { return 1L; }

    static Float floatValueOf() { return 1.0f; }

    static Double doubleValueOf() { return 1.0; }

    static Character charValueOf() { return 1; }

    static Boolean booleanValueOf() { return true; }

    static void castFieldTypes() {
        CastFieldType.i = 1;
        CastFieldType.b = 1;
        CastFieldType.s = 1;
        CastFieldType.c = 1;
        CastFieldType.bool = true;
        CastFieldType.l = 1;
        CastFieldType.f = 1.0f;
        CastFieldType.d = 1.0;

        CastFieldType klass = new CastFieldType();
        klass.im = 1;
        klass.bm = 1;
        klass.sm = 1;
        klass.cm = 1;
        klass.boolm = true;
        klass.lm = 1;
        klass.fm = 1;
        klass.dm = 1;

        Integer i = klass.im;
        Byte b = klass.bm;
        Short s = klass.sm;
        Character c = klass.cm;
        Boolean bool = klass.boolm;
        Long l = klass.lm;
        Float f = klass.fm;
        Double d = klass.dm;

        int i2 = klass.bm;
        int i3 = klass.sm;
        int i4 = klass.cm;
    }

    static class CastFieldType {
        static int i = 1;
        static byte b = 1;
        static short s = 1;
        static char c = 1;
        static boolean bool = true;
        static long l = 1;
        static float f = 1;
        static double d = 1;

        int im = 1;
        byte bm = 1;
        short sm = 1;
        char cm = 1;
        boolean boolm = true;
        long lm = 1;
        float fm = 1;
        double dm = 1;
    }

    static void castArrayElementType() {
        int[] i = new int[1];
        i[0] = 1;
        short[] s = new short[1];
        s[0] = 1;
        byte[] b = new byte[1];
        b[0] = 1;
        char[] c = new char[1];
        c[0] = 1;
        boolean[] bool = new boolean[1];
        bool[0] = true;
        long[] l = new long[1];
        l[0] = 1;
        float[] f = new float[1];
        f[0] = 1;
        double[] d = new double[1];
        d[0] = 1;
    }

    static int variable() {
        int i = 153;
        return i;
    }

    static int unaryMinus() {
        int i = 153;
        return -i;
    }

    static int ifThen() {
        boolean a = true;
        if (a)
            return 2;
        return 1;
    }

    static int ifElse() {
        boolean a = false;
        if (a) {
            return 2;
        }
        else {
            return 1;
        }
    }

    static int loop() {
        int i = 0;
        while (i < 10) i++;
        return i;
    }

    static int loopWithBreak() {
        int i = 0;
        while (true) {
            if (i > 10) break;
            i++;
        }
        return i;
    }

    static int loopWithReturn() {
        int i = 0;
        while (true) {
            if (i > 10) return i;
            i++;
        }
    }

    static int simpleFinally() {
        int i = 5;
        try {
            return i;
        }
        finally {
            i = 3;
        }
    }

    static int simpleFinallyWithReturn() {
        int i = 5;
        try {
            return i;
        }
        finally {
            return 3;
        }
    }

    static int simpleFinallyWithContinueInLoop() {
        int i = 5;
        while (true) {
            try {
                if (i % 2 == 0) continue;
                if (i > 10) return i;
            }
            finally {
                i++;
            }
        }
    }

    static int simpleFinallyWithBreakInLoop() {
        int i = 5;
        while (true) {
            try {
                if (i % 2 == 0) break;
            }
            finally {
                i++;
            }
        }
        return i;
    }

    static Object call() {
        return Integer.valueOf(1);
    }

    static Object callWithObject() {
        return String.valueOf("str");
    }

    static Object getStaticField() {
        return C.FOO;
    }

    static int FIELD = 0;

    static int putStaticField() {
        FIELD = 5;
        int f1 = FIELD;
        FIELD = 6;
        int f2 = FIELD;
        return f2 + f1;
    }

    static class C {
        static String FOO = "FOO";

        int y = 15;

        C(int y) {
            this.y = y;
        }

        C() {}

        int getY() {
            return y;
        }

        static C newC() {
            return new C();
        }

        static void throwException() {
            throw new RuntimeException();
        }
    }

    static int getInstanceField() {
        return C.newC().y;
    }

    static int putInstanceField() {
        C c = C.newC();
        c.y = 5;
        int f1 = c.y;
        c.y = 6;
        int f2 = c.y;
        return f1 + f2;
    }

    static int instanceMethod() {
        return C.newC().getY();
    }

    static int constructorCallNoArgs() {
        return new C().y;
    }

    static int constructorCallWithArgs() {
        return new C(10).y;
    }

    static class MyEx extends RuntimeException {
        final int x;

        MyEx(int x) {
            this.x = x;
        }
    }

    static int tryCatch() {
        try {
            throw new MyEx(10);
        }
        catch (MyEx e) {
            return e.x;
        }
    }

    static int tryWiderCatch() {
        int a = 10;
        try {
            if (a > 0) {
                throw new MyEx(10);
            }
        } catch (Exception e) {
            return ((MyEx) e).x;
        }
        return 2;
    }

    static int classCastException() {
        Object a = "";
        try {
            Integer s = (Integer) a;
        }
        catch (ClassCastException e) {
            return 1;
        }
        return 2;
    }

    static String classLiteral() {
        return String.class.toString();
    }

    static int callThrowingMethod() {
        try {
            C.throwException();
        }
        catch (RuntimeException e) {
            return 1;
        }
        return 0;
    }

    static int NPE() {
        try {
            Object x = null;
            x.toString();
        }
        catch (NullPointerException e) {
            return 1;
        }
        return 0;
    }

    static Class<?> arrayClass() {
        return int[].class;
    }

    static int arrayOfByte() {
        byte[] a = new byte[] {1, 2, 3};
        int sum = 0;
        for (int i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfShort() {
        short[] a = new short[] {1, 2, 3};
        int sum = 0;
        for (int i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfChar() {
        char[] a = new char[] {1, 2, 3};
        int sum = 0;
        for (int i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfInt() {
        int[] a = new int[] {1, 2, 3};
        int sum = 0;
        for (int i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfLong() {
        long[] a = new long[] {1, 2, 3};
        int sum = 0;
        for (long i : a) {
            sum += i;
        }
        return sum;
    }

    static float arrayOfFloat() {
        float[] a = new float[] {1, 2, 3};
        float sum = 0;
        for (float i : a) {
            sum += i;
        }
        return sum;
    }

    static double arrayOfDouble() {
        double[] a = new double[] {1, 2, 3};
        double sum = 0;
        for (double i : a) {
            sum += i;
        }
        return sum;
    }

    static String arrayOfString() {
        String[] a = new String[] {"1", "2", "3"};
        String sum = "";
        for (String i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfByte2() {
        byte[][] a = new byte[][] {{1}, {2}, {3}};
        int sum = 0;
        for (byte[] aa: a)
            for (int i : aa) {
                sum += i;
            }
        return sum;
    }

    static int arrayOfShort2() {
        short[][] a = new short[][] {{1}, {2}, {3}};
        int sum = 0;
        for (short[] aa: a)
            for (int i : aa) {
                sum += i;
            }
        return sum;
    }

    static int arrayOfChar2() {
        char[][] a = new char[][] {{1}, {2}, {3}};
        int sum = 0;
        for (char[] aa: a)
            for (int i : aa) {
                sum += i;
            }
        return sum;
    }

    static int arrayOfInt2() {
        int[][] a = new int[][] {{1}, {2}, {3}};
        int sum = 0;
        for (int[] aa: a)
            for (int i : aa) {
                sum += i;
            }
        return sum;
    }

    static int arrayOfLong2() {
        long[][] a = new long[][] {{1}, {2}, {3}};
        int sum = 0;
        for (long[] aa: a)
            for (long i : aa) {
                sum += i;
            }
        return sum;
    }

    static float arrayOfFloat2() {
        float[][] a = new float[][] {{1}, {2}, {3}};
        float sum = 0;
        for (float[] aa: a)
            for (float i : aa) {
                sum += i;
            }
        return sum;
    }

    static double arrayOfDouble2() {
        double[][] a = new double[][] {{1}, {2}, {3}};
        double sum = 0;
        for (double[] aa: a)
            for (double i : aa) {
                sum += i;
            }
        return sum;
    }

    static String arrayOfString2() {
        String[][] a = new String[][] {{"1"}, {"2"}, {"3"}};
        String sum = "";
        for (String[] aa: a)
            for (String i : aa) {
                sum += i;
            }
        return sum;
    }

    static String multiArrayOfInt() {
        int[][] a = new int[2][2];
        String s = "";
        for (int[] x : a)
            for (int y : x) {
                s += y;
            }
        return s;
    }

    static String multiArrayOfString() {
        String[][] a = new String[2][2];
        for (String[] x : a)
            for (int i = 0; i < x.length; i++) {
                x[i] = i + "";
            }
        String s = "";
        for (String[] x : a)
            for (String y : x) {
                s += y;
            }
        return s;
    }

    static void castToArray() {
        int[] i = (int[]) null;
    }

    static void classNotLoadedException() {
        ClassNotLoadedExceptionTest klass = new ClassNotLoadedExceptionTest();

        klass.f1(null);
        klass.f2(null);
        klass.f3(null);
        klass.f4(null, 1);
        klass.f5(null, null);
        klass.f6(null, null);

        ClassNotLoadedExceptionTest.s1(null);
        ClassNotLoadedExceptionTest.s2(null);
        ClassNotLoadedExceptionTest.s3(null);
        ClassNotLoadedExceptionTest.s4(null, 1);
        ClassNotLoadedExceptionTest.s5(null, null);
        ClassNotLoadedExceptionTest.s6(null, null);

        new ClassNotLoadedExceptionTest(null, null, null);
        new ClassNotLoadedExceptionTest(null, 1, null, null);
    }

    static class ClassNotLoadedExceptionTest {
        ClassNotLoadedExceptionTest() {}
        // instance methods
        static class F1 {}
        void f1(F1 p) {}

        static class F2 {}
        void f2(F2[] p) {}

        static class F3 {}
        void f3(F3[][] p) {}

        static class F4 {}
        void f4(F4[] p, int p2) {}

        static class F5 {}
        void f5(F5[] p, int[] p2) {}

        static class F6 {}
        void f6(F6[] p, int[][] p2) {}

        // static methods
        static class S1 {}
        static void s1(S1 p) {}

        static class S2 {}
        static void s2(S2[] p) {}

        static class S3 {}
        static void s3(S3[][] p) {}

        static class S4 {}
        static void s4(S4[] p, int p2) {}

        static class S5 {}
        static void s5(S5[] p, int[] p2) {}

        static class S6 {}
        static void s6(S6[] p, int[][] p2) {}

        // constructor
        static class C2 {}
        static class C1 {}
        static class C3 {}
        ClassNotLoadedExceptionTest(C1 p, C2[] p2, C3[][] p3) {}

        static class C4 {}
        ClassNotLoadedExceptionTest(C4 p, int p2, int[] p3, int[][] p4) {}
    }

    static void loadLibraryClasses() {
        LoadLibraryClasses klass = new LoadLibraryClasses();

        klass.f1(1);
        klass.f2(LoadLibraryClasses.str());
        klass.f3(LoadLibraryClasses.c());
        klass.f4(LoadLibraryClasses.cl());
    }

    static class LoadLibraryClasses {
        void f1(Integer i) {}
        void f2(String s) {}
        void f3(Class c) {}
        void f4(ClassLoader cl) {}

        static ClassLoader cl() { return null; }
        static String str() { return null; }
        static Class c() { return null; }
    }


    static int numberIntValue() {
        Number n = 1;
        return n.intValue();
    }

    static void getValueFromStack() {
        int i = 1;
        boolean b = true;
        Integer[] IFEQ = new Integer[] { b ? 100 : 200 };
        Integer[] IF_ICMPNE = new Integer[] { i == 1  ? 100 : 200 };

        long l = 1;
        Long[] IFEQ_L = new Long[] { b ? 100L : 200L };
    }

    @Override
    String superCall() {
        return "Derived";
    }

    @IgnoreInReflectionTests
    String testInvokeSpecialForSuperCall() {
        return super.superCall();
    }

    @IgnoreInReflectionTests
    static String testInvokeSpecial() {
        TestData td = new TestData();
        return td.invokeSpecialPrivateFun("");
    }

    private String invokeSpecialPrivateFun(String s) { return "Base"; }

    static Throwable exception1() {
        throw new IllegalStateException();
    }

    static void exception2() {
        new ExceptionsTest().f1();
    }

    static void exceptionClassCast() {
        ExceptionsTest.Derived test = (ExceptionsTest.Derived) new ExceptionsTest.Base();
    }

    static class ExceptionsTest {
        void f1() {
            throw new IllegalStateException();
        }

        static class Base {}
        static class Derived extends Base {}
    }

    static boolean exceptionIndexOutOfBounds() {
        int[] ints = new int[1];
        try { int i = ints[2];           return false; } catch (ArrayIndexOutOfBoundsException e) { }
        try { ints[2] = 1;               return false; } catch (ArrayIndexOutOfBoundsException e) { }

        short[] shorts = new short[1];
        try { short s = shorts[2];       return false; } catch (ArrayIndexOutOfBoundsException e) { }
        try { shorts[2] = 1;             return false; } catch (ArrayIndexOutOfBoundsException e) { }

        char[] chars = new char[1];
        try { char c = chars[2];         return false; } catch (ArrayIndexOutOfBoundsException e) { }
        try { chars[2] = 1;              return false; } catch (ArrayIndexOutOfBoundsException e) { }

        byte[] bytes = new byte[1];
        try { byte b = bytes[2];         return false; } catch (ArrayIndexOutOfBoundsException e) { }
        try { bytes[2] = 1;              return false; } catch (ArrayIndexOutOfBoundsException e) { }

        long[] longs = new long[1];
        try { long l = longs[2];         return false; } catch (ArrayIndexOutOfBoundsException e) { }
        try { longs[2] = 1;              return false; } catch (ArrayIndexOutOfBoundsException e) { }

        double[] doubles = new double[1];
        try { double d = doubles[2];     return false; } catch (ArrayIndexOutOfBoundsException e) { }
        try { doubles[2] = 1.0;          return false; } catch (ArrayIndexOutOfBoundsException e) { }

        float[] floats = new float[1];
        try { float f = floats[2];       return false; } catch (ArrayIndexOutOfBoundsException e) { }
        try { floats[2] = 1;             return false; } catch (ArrayIndexOutOfBoundsException e) { }

        boolean[] booleans = new boolean[1];
        try { boolean bool = booleans[2];return false; } catch (ArrayIndexOutOfBoundsException e) { }
        try { booleans[2] = true;        return false; } catch (ArrayIndexOutOfBoundsException e) { }

        Object[] objects = new Object[1];
        try { Object o = objects[2];     return false; } catch (ArrayIndexOutOfBoundsException e) { }
        try { objects[2] = 1;            return false; } catch (ArrayIndexOutOfBoundsException e) { }

        return true;
    }

    static boolean indexOutOfBoundsForString() {
        String str = "";
        try { str.charAt(10);    return false; } catch (IndexOutOfBoundsException e) { }
        try { str.substring(10); return false; } catch (IndexOutOfBoundsException e) { }

        return true;
    }

    static int coerceByte() {
        byte[] b = new byte[2];
        return b[1];
    }

    @IgnoreInReflectionTests
    public static void invokeMethodWithArrayOfInterfaces() {
        BaseToArray[] c = new BaseToArray[1];

        TestInvokeWithObjectArray.invokeStaticFun(c);
        TestInvokeWithObjectArray.invokeStaticPrivateFun(c);

        TestInvokeWithObjectArray myObj = new TestInvokeWithObjectArray();
        myObj.invokeMemberFun(c);
        myObj.invokeMemberPrivateFun(c);

        int i = TestInvokeWithObjectArray.primitiveReturnValue(c) + 1;
    }

    private static class TestInvokeWithObjectArray {
        public static void invokeStaticFun(Object[] o) {
        }

        public static void invokeStaticPrivateFun(Object[] o) {
        }

        public void invokeMemberFun(Object[] o) {
        }

        public void invokeMemberPrivateFun(Object[] o) {
        }

        public static int primitiveReturnValue(Object[] o) {
            return 1;
        }
    }

    private interface BaseToArray {
    }

    public TestData() {
    }
}




