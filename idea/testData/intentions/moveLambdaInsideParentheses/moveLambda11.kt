// IS_APPLICABLE: true
fun foo() {
    bar<String> <caret>{ it }
}

fun bar<T>(a: (Int)->T): T {
    return a(1)
}