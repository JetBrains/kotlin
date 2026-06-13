// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: util.kt

package util

enum class E {
    A, B
}

val globalE: E = E.A

inline fun fooInline(): Int = when (globalE) {
    E.A -> 1
    E.B -> 2
}

// FILE: test.kt

package test

import util.*

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val LOCAL_VISIBILITY = 5

fun test() = fooInline()

fun fooLocal(): Int = when (globalE) {
    E.A -> 1
    E.B -> 2
}

private fun syntheticClassVisibility(javaClass: Class<*>): Int {
    val extraInt = javaClass.getAnnotation(Metadata::class.java).extraInt
    return (extraInt shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK
}

fun box(): String {
    test()

    val whenMappingsLocal = Class.forName("test.TestKt").declaredClasses.single { it.simpleName == "WhenMappings" }
    val whenMappingsEscaped = Class.forName("util.UtilKt").declaredClasses.single { it.simpleName == "WhenMappings" }

    var visibility = syntheticClassVisibility(whenMappingsLocal)
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5), got $visibility"
    }

    visibility = syntheticClassVisibility(whenMappingsEscaped)
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5), got $visibility"
    }

    return "OK"
}
