class A {
    public void <caret>foo() {

    }

    public void bar(boolean b) {
        foo();
    }

    public void baz() {
        foo();
        bar(false);
    }
}

class B extends A {
    public void foo() {

    }

    public void bar(boolean b) {
        foo();
    }

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

        new C().foo();
        new C().bar(true);
        new C().baz();
    }
}