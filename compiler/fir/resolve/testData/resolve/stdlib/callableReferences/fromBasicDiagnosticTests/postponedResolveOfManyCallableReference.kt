interface A
interface B

fun foo(i: A) {}
fun foo(b: B) {}

fun <T> bar1(f: (T) -> Unit): T = TODO()
fun <T> bar2(f: (T) -> Unit, e: T) {}

fun test(a: A, b: B) {
    val expectedType1: A = bar1(::foo)
    val expectedType2: B = bar1(::foo)

    bar2(::foo, a)
    bar2(::foo, b)
}
