// FIR_IDENTICAL
class Inv<T>

fun <T> foo(x: Inv<T>, y: T) {}
fun <T> foo(x: Inv<T>, f: (T) -> T) {}

fun getOne(x: String): String = ""

fun getTwo(x: String): String = ""
fun getTwo(x: Int): Int = 1

fun test(x: Inv<String>) {
    foo(x, ::getOne)
    foo(x, ::getTwo)
}