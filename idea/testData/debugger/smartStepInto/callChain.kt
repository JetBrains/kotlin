fun foo() {
    A().getB().f1()<caret>
}

class A {
    fun getB() = B()
}

class B {
    fun f1() {}
}

// EXISTS: getB(), f1()