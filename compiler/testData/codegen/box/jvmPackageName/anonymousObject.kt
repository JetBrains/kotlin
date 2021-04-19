// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: anonymousObject.kt
import x.*

fun box(): String =
    "O".z().toString() +
            "K".iz().toString()

// FILE: zao.kt
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:kotlin.jvm.JvmPackageName("xx")
package x

fun String.z(): Any {
    return object {
        override fun toString(): String =
            this@z
    }
}

inline fun String.iz(): Any {
    return object {
        override fun toString(): String =
            this@iz
    }
}

