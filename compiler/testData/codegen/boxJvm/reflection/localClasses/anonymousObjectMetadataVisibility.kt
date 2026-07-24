// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: util.kt

package util

interface A {
    fun result(): String
}

fun objectClass(): Class<*> {
    val anonymousObject = object : A {
        override fun result(): String = "OK"
    }
    return anonymousObject::class.java
}

inline fun objectClassInline(): Class<*> {
    val anonymousObject = object : A {
        override fun result(): String = "OK"
    }
    return anonymousObject::class.java
}

// FILE: test.kt

package test

import util.*

private const val PUBLIC_ABI_FLAG = 1 shl 7

private fun isPublicAbi(javaClass: Class<*>): Boolean =
    javaClass.getAnnotation(Metadata::class.java).extraInt and PUBLIC_ABI_FLAG != 0

fun box(): String {
    if (isPublicAbi(objectClass())) {
        return "Fail: expected non-escaped anonymous object to not be public ABI"
    }

    if (!isPublicAbi(objectClassInline())) {
        return "Fail: expected anonymous object from public inline function to be public ABI"
    }

    return "OK"
}
