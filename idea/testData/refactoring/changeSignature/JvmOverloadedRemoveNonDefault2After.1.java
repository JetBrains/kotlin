class Usages {
    void foo() {
        Foo.foo();
    }

    void fooX() {
        Foo.foo(1);
    }

    void fooXZ() {
        Foo.foo(1, "1");
    }
}