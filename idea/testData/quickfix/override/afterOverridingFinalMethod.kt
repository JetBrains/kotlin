// "Make A.foo open" "true"
open class A {
    open fun foo() {}
}

class B : A() {
    override<caret> fun foo() {}
}