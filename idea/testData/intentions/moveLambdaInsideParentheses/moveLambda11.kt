// IS_APPLICABLE: true
fun foo() {
    bar<String> <caret>{ it.toString() }
}

fun bar<T>(a: (Int)->T): T {
    return a(1)
}
