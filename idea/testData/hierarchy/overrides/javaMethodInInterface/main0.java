interface A {
    public void <caret>foo();
}

class B implements A {
    @Override
    public void foo() {

    }
}

class C implements T {
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