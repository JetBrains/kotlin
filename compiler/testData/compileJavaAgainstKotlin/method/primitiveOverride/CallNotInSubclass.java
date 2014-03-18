package test;

class Test {
    void test() {
        A<Integer> a = new B();
        int ax = a.foo();
        Integer ay = a.foo();
        Object az = a.foo();

        B b = new B();
        int bx = b.foo();
        Integer by = b.foo();
        Object bz = b.foo();
    }
}
