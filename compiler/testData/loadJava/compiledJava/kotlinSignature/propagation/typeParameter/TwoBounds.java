package test;

public interface TwoBounds {

    public interface Super {
        <A extends CharSequence & Cloneable> void foo(A a);
    }

    public interface Sub extends Super {
        <B extends CharSequence & Cloneable> void foo(B b);
    }
}
