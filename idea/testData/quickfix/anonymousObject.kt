// "Make 'object : T {}' abstract" "false"
// ACTION: Implement members
// ACTION: Split property declaration
// ERROR: <html>Object must be declared abstract or implement abstract member<br/><b>internal</b> <b>abstract</b> <b>fun</b> foo(): kotlin.Unit <i>defined in</i> T</html>
trait T {
    fun foo()
}

fun test() {
    val o = <caret>object : T {}
}