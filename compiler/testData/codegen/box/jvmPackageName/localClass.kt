// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: anonymousObject.kt
import x.*

fun box(): String =
    "OK".z().toString()

// FILE: zlc.kt
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:kotlin.jvm.JvmPackageName("xx")
package x

fun String.z(): Any {
    class Local {
        override fun toString(): String =
            this@z
    }

    return Local()
}
