// IS_APPLICABLE: false
fun foo() {
    val x = bar<caret><Any>("x")
}

fun <T> bar(t: T): Int = 1