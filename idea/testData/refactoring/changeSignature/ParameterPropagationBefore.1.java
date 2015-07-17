class J extends A {
    @Override
    public void foo() {

    }

    @Override
    public void bar(boolean b) {
        foo(); // Propagated parameters are not passed to calles in overriding methods
    }

    @Override
    public void baz() {
        foo();
        bar(false);
    }
}

class Test {
    void test() {
        new A().foo();
        new A().bar(true);
        new A().baz();

        new B().foo();
        new B().bar(true);
        new B().baz();

        new J().foo();
        new J().bar(true);
        new J().baz();
    }
}