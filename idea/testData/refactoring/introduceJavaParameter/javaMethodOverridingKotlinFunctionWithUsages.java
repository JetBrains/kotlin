// APPLY_TO_SUPER
class A {
    int x;

    A(int x) {
        this.x = x;
    }
}

interface T {
    int foo(int a, int b, int c);
}

class J extends K implements T {
    @Override
    public int foo(int a, int b, int c) {
        return <selection>new A(a + b)</selection>.x * c;
    }
}

class Test {
    void test() {
        new J().foo(1, 2, 3);
        new K().foo(1, 2, 3);
    }
}