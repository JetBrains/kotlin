// FIR_IDENTICAL
// !CHECK_TYPE

// FILE: StaticOverrides.java

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class StaticOverrides {
    public static class A {
        public static int foo(Runnable x) { return 0; }
        public static boolean foo(Function0<Unit> x) { return true; }
    }

    public static class B {
        public static String foo(Runnable x) { return ""; }
    }

    public static class C extends A {
        public static String foo(Runnable x) { return ""; }
    }
}

// FILE: test.kt

fun test() {
    StaticOverrides.A.foo {} checkType { _<Boolean>() }
    StaticOverrides.B.foo {} checkType { _<String>() }
    StaticOverrides.C.foo {} checkType { _<Boolean>() }
}
