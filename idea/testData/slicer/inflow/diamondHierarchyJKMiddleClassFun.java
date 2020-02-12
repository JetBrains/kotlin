interface A {
    public int foo();
}

interface C extends A {
    public int foo();
}

class D extends B implements C {
    public int foo() {
        return 4;
    }
}