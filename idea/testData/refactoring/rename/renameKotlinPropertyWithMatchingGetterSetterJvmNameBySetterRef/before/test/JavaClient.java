package test;

class Test {
    {
        new A().getFoo();
        new A()./*rename*/setFoo(1);
    }
}