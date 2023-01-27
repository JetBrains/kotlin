// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun <T> Array<T>.getChecked(index: Int): T

expect fun BooleanArray.getChecked(index: Int): Boolean

fun ok() = if (!BooleanArray(1).getChecked(0)) "OK" else "FAIL"

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual fun <T> Array<T>.getChecked(index: Int) = get(index)

actual fun BooleanArray.getChecked(index: Int) = get(index)

fun box() = ok()