package test;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public final class DifferentParametersCount {
    public static class A {
        static public void foo(Runnable x, int y) {}
    }

    public static class B extends A {
        // SAM adapter should not override A.foo
        static public void foo(Runnable x) {}
    }
}
