package test;

public @interface JAnn {
    String valueNew();
}

class Test {
    @JAnn(valueNew = "abc")
    void test1() { }

    @JAnn(valueNew = "abc")
    void test2() { }
}