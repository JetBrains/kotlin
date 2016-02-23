// FILE: 1.kt

import a.OK

fun box(): String {
    val okRef = ::OK

    // TODO: see KT-10892
//    val annotations = okRef.annotations
//    val numAnnotations = annotations.size
//    if (numAnnotations != 1) {
//        return "Failed, annotations: $annotations"
//    }

    return okRef.get()
}

// FILE: 2.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

annotation class A

@A
const val OK: String = "OK"
