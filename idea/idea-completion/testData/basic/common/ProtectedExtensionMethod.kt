// FIR_COMPARISON
abstract class ProtectedTest {
    fun foo() {
        "".<caret>
    }

    abstract protected fun String.bar()
}

// EXIST: bar