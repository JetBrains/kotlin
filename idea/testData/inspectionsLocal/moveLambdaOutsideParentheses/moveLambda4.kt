// IS_APPLICABLE: true
fun foo() {
    bar(2, {
        val x = 3
        it * x
    <caret>})
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}
