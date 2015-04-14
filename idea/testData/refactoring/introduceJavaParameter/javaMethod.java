class A {
    int x;

    A(int x) {
        this.x = x;
    }
}

class J {
    int foo(int a, int b, int c) {
        return <selection>new A(a + b)</selection>.x * c
    }
}

class Test {
    void test() {
        new J().foo(1, 2, 3);
    }
}
