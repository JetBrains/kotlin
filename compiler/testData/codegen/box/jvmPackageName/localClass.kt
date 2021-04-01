// TARGET_BACKEND: JVM
// WITH_RUNTIME
// IGNORE_FIR_DIAGNOSTICS

//  Duplicate JVM class name 'xx/ZKt'
// IGNORE_BACKEND: ANDROID

// FILE: anonymousObject.kt
import x.*

fun box(): String =
    "OK".z().toString()

// FILE: z.kt
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
