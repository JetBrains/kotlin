class A<out T>

typealias AInt = A<Int>

typealias AString = A<String>

fun A<Number>.foo() {}

/**
 * [AInt.fo<caret_1>o]
 * [AString.fo<caret_2>o] - Shouldn't be resolved
 */
fun main() {}