package test;

class Test {
    {
        new A().getFoo();
        new A()./*rename*/setBar(1);
    }
}