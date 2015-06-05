package usages

import library.*

class J extends A {
    public X(int n) {
        super(n);
    }

    static void test() {
        A a = new A();
        A aa = new A(1);
    }
}
