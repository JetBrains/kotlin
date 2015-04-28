// IS_APPLICABLE: false
fun foo() {
    bar() <caret>{ it }
}

fun bar(b: (Int) -> Int) {
    b(1)
}
