package usages

import library.*

class J extends A.T {
    public J() {
    }

    public J(int n) {
        super();
    }

    static void test() {
        A.T a = new A.T();
        A.T aa = new A.T(1);
    }
}