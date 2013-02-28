// "Make overridden member in supertype open" "true"
open class A {
    open fun foo() {}
}

class B : A() {
    override<caret> fun foo() {}
}