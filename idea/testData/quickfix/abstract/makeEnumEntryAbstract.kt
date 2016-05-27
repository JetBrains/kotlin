// "Make 'A' abstract" "false"
// ACTION: Implement members
// ERROR: Class 'A' must be declared abstract or implement abstract member public abstract fun foo(): Unit defined in E

enum class E {
    <caret>A;

    abstract fun foo()
}