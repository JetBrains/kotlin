package test;

import java.lang.CharSequence;

public class WildcardBounds {
    static class A<T> {}

    void foo(A<? extends CharSequence> x, A<? super String> y) {}
}
