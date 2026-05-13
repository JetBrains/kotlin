// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun box(): String {
    fun <@JvmSpecialize T> id(x: T) = x
    if (id(42) != 42) return "fail"
    return "OK"
}
