// FIR_COMPARISON
open class Base

open class A : Base

interface I

class B : A(), I {
    fun foo() {
        super<<caret>
    }
}

// EXIST: A
// EXIST: I
// NOTHING_ELSE