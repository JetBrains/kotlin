// TARGET_BACKEND: JVM
// WITH_STDLIB
// SAM_CONVERSIONS: CLASS

// FILE: util.kt

package util

inline fun samWrapperInline(noinline job: () -> Unit): Class<*> {
    val samWrapper = Runnable(job)
    return samWrapper::class.java
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
    var result = ""
    val job = { result = "OK" }
    val samWrapper = Runnable(job)
    samWrapper.run()

    var visibility = syntheticClassVisibility(samWrapper::class.java)
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5) for non-escaped SAM wrapper, got $visibility"
    }
    if (isPublicAbi(samWrapper::class.java)) {
        return "Fail: expected non-escaped SAM wrapper to not be public ABI"
    }

    val samWrapperInInlineClass = samWrapperInline {}
    visibility = syntheticClassVisibility(samWrapperInInlineClass)
    if (visibility != PUBLIC_VISIBILITY) { // NOTE: in future potentially can be lowered to internal instead of public
        return "Fail: expected PUBLIC visibility (3) for escaped SAM wrapper, got $visibility"
    }
    if (!isPublicAbi(samWrapperInInlineClass)) {
        return "Fail: expected escaped SAM wrapper to be public ABI"
    }

    return result
}
