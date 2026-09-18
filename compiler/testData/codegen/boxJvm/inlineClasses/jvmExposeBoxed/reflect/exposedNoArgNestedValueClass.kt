// WITH_REFLECT
// TARGET_BACKEND: JVM

// The exposed no-arg constructor used to box the underlying value with Integer.valueOf and then
// checkcast it, which threw ClassCastException whenever the underlying type was itself a value class.

@file:OptIn(ExperimentalStdlibApi::class)

package test

@JvmExposeBoxed
@JvmInline
value class NestedUnderlying(val a: UInt = 7u)

@JvmExposeBoxed
@JvmInline
value class PlainUnderlying(val a: Int = 7)

fun box(): String {
    val nested = NestedUnderlying::class.java.getDeclaredConstructor().newInstance() as NestedUnderlying
    if (nested.a != 7u) return "FAIL 1: ${nested.a}"

    val plain = PlainUnderlying::class.java.getDeclaredConstructor().newInstance() as PlainUnderlying
    if (plain.a != 7) return "FAIL 2: ${plain.a}"

    return "OK"
}
