package test;

class ExtendsB extends B {
    void test() {
        byte x = foo();
        Byte y = foo();
        Object z = foo();
    }
}

class ExtendsC extends C {
    void test() {
        byte x = foo();
        Byte y = foo();
        Object z = foo();
    }

    @Override
    public Byte foo() { return 42; }
}
