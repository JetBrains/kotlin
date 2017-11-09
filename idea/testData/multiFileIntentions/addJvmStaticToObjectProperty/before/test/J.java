package test;

class J {
    void test(O o) {
        o.getFoo();
        o.setFoo(1);

        O.INSTANCE.getFoo();
        O.INSTANCE.setFoo(2);
    }
}