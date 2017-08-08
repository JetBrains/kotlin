// IS_APPLICABLE: true
fun foo() {
    bar <caret>{
        it * 3
    }
}

fun bar(a : Int = 2, b: (Int) -> Int) {
    b(a)
}
