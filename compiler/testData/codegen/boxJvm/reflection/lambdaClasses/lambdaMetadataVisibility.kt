// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: util.kt

package util

import kotlin.jvm.JvmSerializableLambda

inline fun fooInline(): Class<*> {
    val lambda = @JvmSerializableLambda { "OK" }
    return lambda::class.java
}

// FILE: test.kt

package test

import util.*

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val PUBLIC_ABI_FLAG = 1 shl 7
private const val PUBLIC_VISIBILITY = 3
private const val LOCAL_VISIBILITY = 5

private fun metadataExtraInt(javaClass: Class<*>): Int =
    javaClass.getAnnotation(Metadata::class.java).extraInt

private fun syntheticClassVisibility(javaClass: Class<*>): Int {
    val extraInt = metadataExtraInt(javaClass)
    return (extraInt shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK
}

private fun isPublicAbi(javaClass: Class<*>): Boolean =
    metadataExtraInt(javaClass) and PUBLIC_ABI_FLAG != 0

fun box(): String {
    val lambda = @JvmSerializableLambda { "OK" }

    var visibility = syntheticClassVisibility(lambda::class.java)
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5) for non-escaped lambda, got $visibility"
    }
    if (isPublicAbi(lambda::class.java)) {
        return "Fail: expected non-escaped lambda to not be public ABI"
    }

    visibility = syntheticClassVisibility(fooInline())
    if (visibility != PUBLIC_VISIBILITY) { // NOTE: in future potentially can be lowered to internal instead of public
        return "Fail: expected PUBLIC visibility (3) for escaped lambda, got $visibility"
    }
    if (!isPublicAbi(fooInline())) {
        return "Fail: expected escaped lambda to be public ABI"
    }

    return lambda()
}
