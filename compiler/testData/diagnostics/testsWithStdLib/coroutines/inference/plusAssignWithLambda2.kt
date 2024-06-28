// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

open class A

operator fun <T> T.plus(x: (T) -> Int) = A()
operator fun <T> T.plusAssign(x: (Int) -> T) {}

fun foo(total: Int) = 10
fun foo(total: Float) = 10
fun foo(total: String) = 10

fun <T> id(x: T) = x

fun main() {
    var newValue = A()
    newValue += id { total -> A() }
    newValue += id(fun(total) = A())
    newValue += id(fun(total): A { return A() })
    newValue += id(::foo)
}
