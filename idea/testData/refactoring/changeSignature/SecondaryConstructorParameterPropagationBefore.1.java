class J extends A {
    public J() {

    }

    public J(boolean b) {
        super();
    }
}

class Test {
    void test() {
        new A();
        new A(true);
        new B();
        new B(true);
        new C(true);
        new J();
        new J(true);
    }
}