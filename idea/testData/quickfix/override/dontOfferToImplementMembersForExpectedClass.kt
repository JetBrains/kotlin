// "Implement members" "false"
// ACTION: Create test
// ACTION: Make internal
// ACTION: Make private
// ACTION: Move 'A' to separate file

interface I {
    fun foo()
}

@Suppress("UNSUPPORTED_FEATURE")
expect <caret>class A : I
