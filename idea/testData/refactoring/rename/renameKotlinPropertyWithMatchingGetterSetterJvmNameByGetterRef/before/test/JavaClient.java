package test;

class Test {
    {
        new A()./*rename*/getFoo();
        new A().setFoo(1);
    }
}