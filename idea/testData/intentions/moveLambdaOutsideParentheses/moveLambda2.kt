// IS_APPLICABLE: true
fun foo() {
    bar<caret>(a = 2, b = { it })
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}
