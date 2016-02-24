// FILE: A.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

fun foo(): String = "OK"
const val constOK: String = "OK"
val valOK: String = "OK"
var varOK: String = "Hmmm?"

// FILE: B.kt

import a.*

fun main(args: Array<String>) {
    if (foo() != "OK") throw AssertionError("Fail function")
    if (constOK != "OK") throw AssertionError("Fail const")
    if (valOK != "OK") throw AssertionError("Fail val")
    varOK = "OK"
    if (varOK != "OK") throw AssertionError("Fail var")
}
