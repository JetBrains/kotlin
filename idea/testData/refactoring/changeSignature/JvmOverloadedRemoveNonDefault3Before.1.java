class Usages {
    void foo() {
        Foo.foo("1");
    }

    void fooX() {
        Foo.foo(1, "1");
    }

    void fooXY() {
        Foo.foo(1, 1.0, "1");
    }
}