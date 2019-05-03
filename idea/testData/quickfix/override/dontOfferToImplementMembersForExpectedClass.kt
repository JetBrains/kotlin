// "Implement members" "false"
// ACTION: Create test
// ACTION: Make internal
// ACTION: Extract 'A' from current file

interface I {
    fun foo()
}

@Suppress("UNSUPPORTED_FEATURE")
expect <caret>class A : I
