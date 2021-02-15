class View

context(View) val Int.dp get() = 42 * this

fun View.f() {
    123.dp
    with(123) {
        dp
    }
}

fun Int.g(v: View) {
    with(v) {
        dp
    }
}

fun h() {
    123.dp
}