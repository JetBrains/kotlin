// TARGET_BACKEND: JVM
// WITH_STDLIB
// !INHERIT_MULTIFILE_PARTS

// FILE: anonymousObject.kt
import x.*

fun box(): String =
    "O".z().toString() +
            "K".iz().toString()

// FILE: z.kt
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("Z2")
@file:kotlin.jvm.JvmPackageName("xx")
package x

fun String.z(): Any {
    return object {
        override fun toString(): String =
            this@z
    }
}

// FILE: z2.kt
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("Z2")
@file:kotlin.jvm.JvmPackageName("xx")
package x

inline fun String.iz(): Any {
    return object {
        override fun toString(): String =
            this@iz
    }
}
