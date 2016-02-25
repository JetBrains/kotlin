// "Make 'object : T {}' abstract" "false"
// ACTION: Implement members
// ACTION: Split property declaration
// ERROR: Object must be declared abstract or implement abstract member public abstract fun foo(): Unit defined in T
interface T {
    fun foo()
}

fun test() {
    val o = <caret>object : T {}
}