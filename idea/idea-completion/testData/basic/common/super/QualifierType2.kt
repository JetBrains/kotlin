// FIR_COMPARISON
open class A

interface I

class B : A(), I {
    fun foo() {
        super<<caret>>
    }
}

// EXIST: A
// EXIST: I
// NOTHING_ELSE