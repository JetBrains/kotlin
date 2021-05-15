// FIR_COMPARISON
open class Base {
    open fun foo(p: Int){}
}

class Derived : Base() {
    override fun foo(p: Int) {
        super.<caret>
    }
}

// ELEMENT: foo
// TAIL_TEXT: "(p: Int)"