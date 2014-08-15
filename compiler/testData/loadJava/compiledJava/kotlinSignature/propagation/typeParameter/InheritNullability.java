package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface InheritNullability {

    public interface Super {
        @KotlinSignature("fun <A: CharSequence> foo(a: A)")
        <A extends CharSequence> void foo(A a);
    }

    public interface Sub extends Super {
        <B extends CharSequence> void foo(B b);
    }
}
