package test;

class Test {
    {
        new A()./*rename*/foo();
        new A().bar(1);
    }
}