package test;

public interface TwoSuperclasses {

    public interface Super1 {
        <A extends CharSequence> void foo(A a);
    }

    public interface Super2 {
        <B extends CharSequence> void foo(B a);
    }

    public interface Sub extends Super1, Super2 {
        <C extends CharSequence> void foo(C c);
    }
}
