package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface TwoTypeParameters {

    public interface Super {
        @KotlinSignature("fun <A: CharSequence, B: Cloneable> foo(a: A, b: B)")
        <A extends CharSequence, B extends Cloneable> void foo(A a, B b);
    }

    public interface Sub extends Super {
        <B extends CharSequence, A extends Cloneable> void foo(B b, A a);
    }
}
