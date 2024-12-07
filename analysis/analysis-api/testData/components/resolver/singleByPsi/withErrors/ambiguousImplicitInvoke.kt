fun x(c: Char) {}

fun call(x: kotlin.Int) {
    operator fun Int.invoke(a: String) {}
    operator fun Int.invoke(b: Boolean) {}
    <expr>x()</expr>
}