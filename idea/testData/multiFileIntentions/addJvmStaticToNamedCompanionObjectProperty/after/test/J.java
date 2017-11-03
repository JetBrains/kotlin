package test;

class J {
    void test(C.O companion) {
        companion.getFoo();
        companion.setFoo(1);

        C.getFoo();
        C.setFoo(2);
    }
}