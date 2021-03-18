// "Make 'A' 'abstract'" "false"
// ACTION: Implement members
// ERROR: Class 'A' is not abstract and does not implement abstract member public abstract fun foo(): Unit defined in E

enum class E {
    <caret>A;

    abstract fun foo()
}
/* FIR_COMPARISON */