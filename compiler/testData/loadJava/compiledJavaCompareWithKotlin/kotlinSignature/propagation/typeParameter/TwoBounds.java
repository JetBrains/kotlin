package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

import java.lang.Cloneable;

public interface TwoBounds {

    public interface Super {
        @KotlinSignature("fun <A: CharSequence> foo(a: A) where A: Cloneable")
        <A extends CharSequence & Cloneable> void foo(A a);
    }

    public interface Sub extends Super {
        <B extends CharSequence & Cloneable> void foo(B b);
    }
}
