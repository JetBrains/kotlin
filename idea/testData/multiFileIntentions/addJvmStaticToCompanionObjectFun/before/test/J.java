package test;

class J {
    void test(C.Companion companion) {
        companion.foo("x");
        C.Companion.foo("y");
    }
}