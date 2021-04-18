// FIR_IDENTICAL
open class A {
    open external fun foo()
}

class B : A() {
    <caret>
}