fun foo(i: Int) {
    <caret>O.foo()
}

class O {
    companion object {
        fun foo() {}
    }
}
// EXPECTED: null