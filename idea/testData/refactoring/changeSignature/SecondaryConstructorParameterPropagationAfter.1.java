class J extends A {
    public J(int n) {

        super(n);
    }

    public J(boolean b, int n) {
        super(n);
    }
}

class Test {
    void test(int n) {
        new A(n);
        new A(true, n);
        new B(n);
        new B(true, n);
        new C(true, n);
        new J(n);
        new J(true, n);
    }
}