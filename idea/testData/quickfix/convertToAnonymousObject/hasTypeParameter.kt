// "Convert to anonymous object" "false"
// ERROR: Interface I does not have constructors
// ACTION: Introduce import alias
// ACTION: Split property declaration
interface I {
    fun <T> foo(): String
}

fun test() {
    val i = <caret>I { "" }
}