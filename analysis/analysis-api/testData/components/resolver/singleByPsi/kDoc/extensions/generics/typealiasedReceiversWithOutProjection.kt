class A<out T>

typealias AInt = A<Int>

typealias ANumber = A<Number>

fun ANumber.foo() {}

/**
 * [AInt.f<caret>oo]
 */
fun main() {}