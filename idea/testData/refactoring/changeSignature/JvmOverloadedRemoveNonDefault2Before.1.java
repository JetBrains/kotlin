class Usages {
    void foo() {
        Foo.foo(1.0);
    }

    void fooX() {
        Foo.foo(1, 1.0);
    }

    void fooXZ() {
        Foo.foo(1, 1.0, "1");
    }
}