package test;

public interface RawTypeWithUpperBound {

    public interface Foo<T extends CharSequence> {
    }

    interface Bar {
        void f(Foo f);
    }
}
