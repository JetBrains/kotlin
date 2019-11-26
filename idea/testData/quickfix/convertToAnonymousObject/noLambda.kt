// "Convert to anonymous object" "false"
// ACTION: Introduce import alias
// ACTION: Split property declaration
// ERROR: Interface I does not have constructors
interface I {
    fun foo(): String
}

fun test() {
    val i = <caret>I()
}