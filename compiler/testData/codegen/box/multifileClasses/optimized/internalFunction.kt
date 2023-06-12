// TARGET_BACKEND: JVM
// WITH_STDLIB
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
