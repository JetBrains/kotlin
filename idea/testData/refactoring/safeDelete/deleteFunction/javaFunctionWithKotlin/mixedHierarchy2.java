interface A {
    public void foo();
}

abstract class C implements B {
    @Override
    public void <caret>foo() {

    }
}