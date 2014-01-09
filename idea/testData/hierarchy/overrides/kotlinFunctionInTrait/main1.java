interface A extends T {
    @Override
    public void foo();
}

class B implements T {
    @Override
    public void foo() {

    }
}

class C implements A {
    @Override
    public void foo() {

    }
}

class D extends Z {
    @Override
    public void foo() {

    }
}

class S {

}