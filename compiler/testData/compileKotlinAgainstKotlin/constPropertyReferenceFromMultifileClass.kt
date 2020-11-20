// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: A.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

annotation class A

@A
const val OK: String = "OK"

// FILE: B.kt

import a.OK

fun box(): String {
    val okRef = ::OK

    val annotations = okRef.annotations
    if (annotations.size != 1) {
        throw AssertionError("Failed, annotations: $annotations")
    }

    return okRef.get()
}
