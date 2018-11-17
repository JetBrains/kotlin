// "Make 'A' abstract" "false"
// ERROR: Class 'A' is not abstract and does not implement abstract member public abstract fun foo(): String defined in I
// ERROR: Primary constructor is required for inline class
// ACTION: Create test
// ACTION: Implement members
// ACTION: Move 'A' to separate file
// ACTION: Rename file to A.kt
interface I {
    fun foo(): String
}

inline class A<caret> : I