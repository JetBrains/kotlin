package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface TwoSuperclasses {

    public interface Super1 {
        @KotlinSignature("fun <A: CharSequence> foo(a: A)")
        <A extends CharSequence> void foo(A a);
    }

    public interface Super2 {
        @KotlinSignature("fun <B: CharSequence> foo(a: B)")
        <B extends CharSequence> void foo(B a);
    }

    public interface Sub extends Super1, Super2 {
        <C extends CharSequence> void foo(C c);
    }
}
