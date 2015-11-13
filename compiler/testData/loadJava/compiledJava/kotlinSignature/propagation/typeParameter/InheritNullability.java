package test;

public interface InheritNullability {

    public interface Super {
        <A extends CharSequence> void foo(A a);
    }

    public interface Sub extends Super {
        <B extends CharSequence> void foo(B b);
    }
}
