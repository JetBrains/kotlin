// one.VarargAndFullValueClassKt
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

package one

value class IntPair(val first: Int, val second: Int)

fun foo(vararg varargParam: String, valueParam: IntPair) = Unit
fun bar(vararg valueParams: IntPair) = Unit
