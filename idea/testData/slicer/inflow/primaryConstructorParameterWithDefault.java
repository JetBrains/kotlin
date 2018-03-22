class D extends A {
    D(int n, String s) {
        super(n, s);
    }

    D(int n) {
        super(n);
    }

    void test() {
        new A(1);
        new A(1, "2");
    }
}