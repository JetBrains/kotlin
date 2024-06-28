fun Int.foo(a: Int): Int = this + a
fun Int?.bar(b: kotlin.Int): Int = this ?: b

fun usage() {
    (1.foo(2).bar(3) as? Int)?.foo(4.bar(5).bar(6)).bar(0)
}