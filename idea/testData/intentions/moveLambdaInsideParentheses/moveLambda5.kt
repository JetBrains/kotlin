// IS_APPLICABLE: true
fun foo() {
    bar() <caret>{ it }
}

fun bar(b: Int->Int) {
    return b(a)
}