fun foo(a: Int.()-> String): String{ return a(1) }
fun Number.test1(): String { return "O" }
fun test2(a: Number): String { return "K" }

fun box(): String {
    return foo(Number::test1) + foo(::test2)
}