// "Implement members" "false"
// ACTION: Make internal
// ACTION: Extract 'A' from current file

interface I {
    fun foo()
}

@Suppress("UNSUPPORTED_FEATURE")
expect <caret>class A : I
