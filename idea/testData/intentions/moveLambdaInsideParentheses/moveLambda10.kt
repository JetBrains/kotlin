// IS_APPLICABLE: true
fun foo() {
    bar<String>("x") <caret>{ it }
}

fun bar<T>(t:T, a: (Int) -> Int): Int {
    return a(1)
}
