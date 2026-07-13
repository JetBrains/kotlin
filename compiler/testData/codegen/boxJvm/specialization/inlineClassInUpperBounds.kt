// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

@JvmInline
value class I(val value: Int)

fun <@JvmSpecialize T: I> idI(x: T) = x

fun box(): String {
    if (idI(I(42)).value != 42) return "fail"
    return "OK"
}
