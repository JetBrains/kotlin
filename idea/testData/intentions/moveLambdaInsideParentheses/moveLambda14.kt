// IS_APPLICABLE: true
fun foo() {
    bar(1) <caret>{
        it * 3
    }
}

fun bar(c: Int, a: Int = 2, b: (Int) -> Int) {
    b(a)
}
