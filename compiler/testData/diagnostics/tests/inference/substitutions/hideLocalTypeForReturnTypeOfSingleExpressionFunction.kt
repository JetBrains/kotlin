// FIR_IDENTICAL
// ISSUE: KT-51418

abstract class A<T>
private fun fPrivate() = test("private")
fun fPublic() = test("public")

private fun <T> test(t: T) = object : A<T>() {
    fun bar(): T = t
}

fun main() {
    fPrivate().bar()
    fPublic().<!UNRESOLVED_REFERENCE!>bar<!>()
    test(1).bar()
}

class FieldTest {
    var result = ""

    private val test = object {
        fun bar() = object {
            fun qux() = object {
            }.also { result += "b" }
        }.also { result += "a" }
    }.also { result += "!" }

    private val ttt = test.bar()
    private val qqq = ttt.qux()
}

interface I<T>
interface I2<A, B>

private fun <V> f(x: V) = object : I<I<V>> {}
private fun <V> f1(x: V) = object : I<I<in V>> {}
private fun <V> f2(x: V) = object : I2<V, V> {}
private fun <V, W> f3(x: V, y: W) = object : I<I2<*, W>> {}

fun g() = f("f")
fun g1() = f("f1")
fun g2() = f2("f2")
fun g3() = f3("f3", 3)
