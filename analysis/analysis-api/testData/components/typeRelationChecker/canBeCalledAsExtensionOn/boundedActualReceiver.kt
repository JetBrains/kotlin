fun <T: X, X: R, R: Number> T.ex<caret>t() {}

fun <T: X, X: R, R: Int> usage(x: T) {
    <expr>x</expr>
}