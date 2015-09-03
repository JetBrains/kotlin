interface X

interface I : X {
    fun foo() {
        super<<caret>
    }
}

// EXIST: Any
// EXIST: X
// NOTHING_ELSE