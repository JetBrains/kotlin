// "Implement as constructor parameters" "false"
// ACTION: Create test
// ACTION: Implement members
// ACTION: Make internal
// ACTION: Make private
// ACTION: Move 'A' to separate file
// ERROR: Object 'A' must be declared abstract or implement abstract member public abstract val foo: Int defined in I
interface I {
    val foo: Int
}

<caret>object A : I
