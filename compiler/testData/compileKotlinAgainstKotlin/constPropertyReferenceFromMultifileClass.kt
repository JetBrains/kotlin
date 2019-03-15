// IGNORE_BACKEND: JVM_IR
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

    // TODO: see KT-10892
//    val annotations = okRef.annotations
//    val numAnnotations = annotations.size
//    if (numAnnotations != 1) {
//        throw AssertionError("Failed, annotations: $annotations")
//    }

    val result = okRef.get()
    return result
}
