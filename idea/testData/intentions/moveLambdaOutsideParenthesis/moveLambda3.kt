// IS_APPLICABLE: true
fun foo() {
    bar<caret>({ it })
}

fun bar(a: Int, b: Int->Int) {
    return b(a)
}