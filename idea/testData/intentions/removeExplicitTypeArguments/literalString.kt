// IS_APPLICABLE: true
fun foo() {
    bar<caret><String>("x")
}

fun <T> bar(t: T): Int = 1