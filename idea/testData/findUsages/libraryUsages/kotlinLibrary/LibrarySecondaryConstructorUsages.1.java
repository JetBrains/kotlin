package usages

import library.*

class J extends A {
    public J() {
    }

    public J(int n) {
        super();
    }

    static void test() {
        A a = new A();
        A aa = new A(1);
    }
}