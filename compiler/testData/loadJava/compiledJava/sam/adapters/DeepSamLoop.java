package test;

public interface DeepSamLoop {

    interface Foo {
        void foo(Bar p);
    }

    interface Bar {
        void foo(Foo p);
    }
}
