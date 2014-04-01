// IS_APPLICABLE: true
fun foo() {
    bar<caret>(2, {
        val x = 3
        it * x
    })
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}
