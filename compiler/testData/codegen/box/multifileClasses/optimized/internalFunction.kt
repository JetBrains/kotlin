// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// WITH_RUNTIME
// !INHERIT_MULTIFILE_PARTS

// FILE: test.kt

@file:JvmMultifileClass
@file:JvmName("Test")

internal fun <T> List<T>.first(): T = get(0)

fun test(): String {
    return listOf("OK").first()
}

// FILE: box.kt

fun box(): String = test()
