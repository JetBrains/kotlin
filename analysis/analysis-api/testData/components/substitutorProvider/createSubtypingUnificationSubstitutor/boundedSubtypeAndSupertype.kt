fun <T: X, X: R, R: Number> T.ext() {
    thi<caret_1_right>s
}

fun <A: B, B: C, C: Int> usage(xx: A) {
    x<caret_1_left>x
}
