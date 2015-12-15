package test;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public final class StaticOverrides {
    public static class A {
        static public void foo(Function0<Unit> x) {}
    }

    public static class B extends A {
        // SAM adapter should not override A.foo
        static public void foo(Runnable x) {}
    }
}
