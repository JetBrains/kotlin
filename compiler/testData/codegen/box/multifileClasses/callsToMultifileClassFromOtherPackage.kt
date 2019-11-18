// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: 1.kt

import a.*

fun box(): String {
    if (foo() != "OK") return "Fail function"
    if (constOK != "OK") return "Fail const"
    if (valOK != "OK") return "Fail val"
    varOK = "OK"
    if (varOK != "OK") return "Fail var"

    return "OK"
}

// FILE: 2.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

fun foo(): String = "OK"
const val constOK: String = "OK"
val valOK: String = "OK"
var varOK: String = "Hmmm?"
