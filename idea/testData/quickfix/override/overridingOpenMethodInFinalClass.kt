// "Make A.foo open" "false"
// ACTION: Convert to expression body
// ERROR: This type is final, so it cannot be inherited from
class A() {
    open fun foo() {}
}

class B : A() {
    override<caret> fun foo() { }
}