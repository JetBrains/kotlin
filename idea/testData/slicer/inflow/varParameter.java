class D extends A {
    D(int n) {
        super(n);
    }

    void test() {
        A a = new A(3);
        int foo = a.getN();
        a.setN(4);
    }
}