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
private const val PUBLIC_ABI_FLAG = 1 shl 7

fun test() = fooInline()

fun fooLocal(): Int = when (globalE) {
    E.A -> 1
    E.B -> 2
}

private fun metadataExtraInt(javaClass: Class<*>): Int =
    javaClass.getAnnotation(Metadata::class.java).extraInt

private fun syntheticClassVisibility(javaClass: Class<*>): Int =
    (metadataExtraInt(javaClass) shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK

private fun isPublicAbi(javaClass: Class<*>): Boolean =
    metadataExtraInt(javaClass) and PUBLIC_ABI_FLAG != 0

fun box(): String {
    test()

    val whenMappingsLocal = Class.forName("test.TestKt").declaredClasses.single { it.simpleName == "WhenMappings" }
    val whenMappingsEscaped = Class.forName("util.UtilKt").declaredClasses.single { it.simpleName == "WhenMappings" }

    var visibility = syntheticClassVisibility(whenMappingsLocal)
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5) for local WhenMappings, got $visibility"
    }
    if (isPublicAbi(whenMappingsLocal)) {
        return "Fail: expected WhenMappings to not be public ABI"
    }

    visibility = syntheticClassVisibility(whenMappingsEscaped)
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5) for escaped WhenMappings, got $visibility"
    }
    if (!isPublicAbi(whenMappingsEscaped)) {
        return "Fail: expected WhenMappings from public inline function to be public ABI"
    }

    return "OK"
}
