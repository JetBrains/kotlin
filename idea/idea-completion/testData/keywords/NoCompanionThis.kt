// FIR_IDENTICAL
// FIR_COMPARISON
class C {
    fun foo() {
        <caret>
    }

    companion object {
    }
}

// INVOCATION_COUNT: 1
// EXIST: "this"
// ABSENT: "this@Companion"
