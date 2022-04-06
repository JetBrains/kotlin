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