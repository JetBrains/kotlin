package test;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface UseParameterAsUpperBound {

    public interface Super {
        @KotlinSignature("fun <A, B: A> foo(a: A, b: B)")
        <A, B extends A> void foo(A a, B b);
    }

    public interface Sub extends Super {
        <B, A extends B> void foo(B b, A a);
    }
}
