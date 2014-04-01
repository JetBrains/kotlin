// IS_APPLICABLE: true
fun foo() {
    bar<caret>(2, { it })
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}
