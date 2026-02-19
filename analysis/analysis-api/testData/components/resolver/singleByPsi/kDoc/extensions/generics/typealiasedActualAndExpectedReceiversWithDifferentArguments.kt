class A<out T>

typealias AInt = A<Int>

typealias AString = A<String>

fun AString.foo() {}

/**
 * [AInt.fo<caret>o] -- Shouldn't be resolved
 */
fun main() {}