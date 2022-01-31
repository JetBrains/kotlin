fun <T> T.foo(): (a: T) -> Unit = TODO()

fun call() {
    val x = 123.foo()
    <expr>x(1)</expr>
}
