// "Make 'Some' abstract" "false"
// ACTION: Create test
// ACTION: Implement members
// ACTION: Move 'Some' to separate file
// ACTION: Rename file to Some.kt
// ERROR: Object 'Some' must be declared abstract or implement abstract member public abstract fun foo(): Unit defined in T
interface T {
    fun foo()
}

object <caret>Some : T