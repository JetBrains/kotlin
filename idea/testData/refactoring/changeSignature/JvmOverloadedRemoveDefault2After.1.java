class Usages {
    void foo() {
        Foo.foo();
    }

    void fooX() {
        Foo.foo(1);
    }

    void fooXY() {
        Foo.foo(1);
    }

    void fooXYZ() {
        Foo.foo(1, "1");
    }
}