package test;

class J {
    void test(O o) {
        o.foo("x");
        O.INSTANCE.foo("y");
    }
}