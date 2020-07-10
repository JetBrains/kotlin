// IS_APPLICABLE: false
fun foo(i: Int) {
    <caret>if (i == 1) {
        bar(1)
    } else if (i == 2) {
        bar(2)
    } else {
        bar(3)
    }
}

fun bar(i: Int) {}