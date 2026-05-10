// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: util.kt

package util

fun foo(): String = "OK"

inline fun fooInline(): Class<*> {
    val callableReferenceInInline = ::foo
    return callableReferenceInInline::class.java
}

// FILE: test.kt

package test

import util.*

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val PUBLIC_VISIBILITY = 3
private const val LOCAL_VISIBILITY = 5

private fun syntheticClassVisibility(javaClass: Class<*>): Int {
    val extraInt = javaClass.getAnnotation(Metadata::class.java).extraInt
    return (extraInt shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK
}

fun box(): String {
    val callableReference = ::foo

    var visibility = syntheticClassVisibility(callableReference::class.java)
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5) for non-escaped callable reference, got $visibility"
    }

    visibility = syntheticClassVisibility(fooInline())
    if (visibility != PUBLIC_VISIBILITY) { // NOTE: in future potentially can be lowered to internal instead of public
        return "Fail: expected PUBLIC visibility (3) for escaped callable reference, got $visibility"
    }

    return callableReference()
}
