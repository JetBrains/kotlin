fun <T, U> operateLambda(p: T, f: (T) -> U) = f(p)
fun returnLabel() {
    <caret>operateLambda(1) {}
}