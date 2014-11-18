class X extends A {
    @Override
    String foo(int n, String s) {
        return "";
    }
}

class Y extends X {
    @Override
    String foo(int n, String s) {
        return super.foo(n, s);
    }
}

class Test {
    void test() {
        new A().foo(1, "abc");
        new B().foo(2, "abc");
        new X().foo(3, "abc");
        new Y().foo(4, "abc");
    }
}