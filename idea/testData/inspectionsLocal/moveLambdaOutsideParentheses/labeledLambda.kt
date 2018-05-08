// IS_APPLICABLE: true
fun foo() {
    bar(2, <caret>l@{ it })
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}
