// FIR_COMPARISON
class C {
    companion object {
        fun String.foo() {
            <caret>
        }
    }
}

// INVOCATION_COUNT: 1
// EXIST: "this@Companion"
