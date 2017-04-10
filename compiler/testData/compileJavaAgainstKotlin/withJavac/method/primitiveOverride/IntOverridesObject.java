package test;

class ExtendsB extends B {
    void test() {
        int x = foo();
        Integer y = foo();
        Object z = foo();
    }
}

class ExtendsC extends C {
    void test() {
        int x = foo();
        Integer y = foo();
        Object z = foo();
    }

    @Override
    public Integer foo() { return 42; }
}
