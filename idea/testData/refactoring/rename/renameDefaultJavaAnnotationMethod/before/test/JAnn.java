package test;

public @interface JAnn {
    String /*rename*/value();
}

class Test {
    @JAnn("abc")
    void test1() { }

    @JAnn(value = "abc")
    void test2() { }
}