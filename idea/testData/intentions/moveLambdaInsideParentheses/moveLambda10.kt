// IS_APPLICABLE: true
fun foo() {
    bar<String>("x") <caret>{ it }
}

fun <T> bar(t:T, a: (Int) -> Int): Int {
    return a(1)
}
