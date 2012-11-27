package test;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface UseParameterInUpperBound {

    public interface Super {
        @KotlinSignature("fun <A, B: List<A>> foo(a: A, b: B)")
        <A, B extends List<A>> void foo(A a, B b);
    }

    public interface Sub extends Super {
        <B, A extends List<B>> void foo(B b, A a);
    }
}
