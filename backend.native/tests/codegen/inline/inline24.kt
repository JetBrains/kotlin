fun foo() = println("foo")
fun bar() = println("bar")

inline fun baz(x: Unit = foo(), y: Unit) {}

fun main(args: Array<String>) {
    baz(y = bar())
}
