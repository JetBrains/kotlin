// IS_APPLICABLE: false
fun test(): String {
    val foo = foo()
    <caret>if (foo !is String?) return "0"
    return "1"
}

fun foo(): Any? = null