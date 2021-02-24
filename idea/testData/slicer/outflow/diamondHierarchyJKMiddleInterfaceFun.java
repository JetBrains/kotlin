interface A {
    public int foo();
}

class B implements A {
    public int foo() {
        return 2;
    }
}

class D extends B implements C {
    public int foo() {
        return 4;
        }
}