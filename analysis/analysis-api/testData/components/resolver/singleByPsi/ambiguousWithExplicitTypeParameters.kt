fun <T> function(t: T, a: Char) {}
fun <U> function(u: U, b: Boolean) {}
fun <V> function(v: V, c: String) {}

fun call() {
    <expr>function<Int>(1)</expr>
}
