package test;

class ExtendsB extends B {
    @Override
    public Integer foo() {
        return 239;
    }

    void test() {
        int x = foo();
        Integer y = foo();
        Object z = foo();
    }
}
