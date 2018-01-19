interface A {
    public int foo();
}

class B implements A {
    public int foo() {
        return 2;
    }
}

interface C extends A {
    public int foo();
}