package test;

import java.util.List;

public interface UseParameterInUpperBound {

    public interface Super {
        <A, B extends List<A>> void foo(A a, B b);
    }

    public interface Sub extends Super {
        <B, A extends List<B>> void foo(B b, A a);
    }
}
