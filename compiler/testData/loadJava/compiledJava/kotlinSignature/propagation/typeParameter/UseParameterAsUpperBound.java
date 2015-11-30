package test;

public interface UseParameterAsUpperBound {

    public interface Super {
        <A, B extends A> void foo(A a, B b);
    }

    public interface Sub extends Super {
        <B, A extends B> void foo(B b, A a);
    }
}
