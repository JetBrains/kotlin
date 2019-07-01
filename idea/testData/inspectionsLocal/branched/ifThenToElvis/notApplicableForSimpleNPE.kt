// PROBLEM: none
// WITH_RUNTIME
// DISABLE-ERRORS
fun main(args: Array<String>) {
    val t: String? = "abc"
    if (t == null<caret>) throw NullPointerException() else t
}
