// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'any{}'"
// IS_APPLICABLE_2: false
fun foo(): Boolean {
    val foo = listOf(true, true)
    <caret>for (e in foo) {
        if (!(f1(e) && f2(e))) return false
    }
    return true
}

fun f1(b: Boolean): Boolean = TODO()
fun f2(b: Boolean): Boolean = TODO()