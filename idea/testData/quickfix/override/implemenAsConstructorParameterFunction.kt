// "Implement as constructor parameters" "false"
// ACTION: Create test
// ACTION: Implement members
// ACTION: Make 'A' abstract
// ACTION: Make internal
// ACTION: Make private
// ACTION: Extract 'A' from current file
// ERROR: Class 'A' is not abstract and does not implement abstract member public abstract fun foo(): Int defined in I
interface I {
    fun foo(): Int
}

<caret>class A : I
