// "Convert to anonymous object" "false"
// ERROR: Interface I does not have constructors
interface I {
    fun foo(): String
    fun bar(): Unit
}

fun test() {
    <caret>I {
    }
}