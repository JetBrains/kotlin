package test;

public interface TwoTypeParameters {

    public interface Super {
        <A extends CharSequence, B extends Cloneable> void foo(A a, B b);
    }

    public interface Sub extends Super {
        <B extends CharSequence, A extends Cloneable> void foo(B b, A a);
    }
}
