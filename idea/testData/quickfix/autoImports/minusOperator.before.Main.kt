// "Import" "true"
// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: <br>public fun String.minus(i: Integer): String defined in h in file minusOperator.before.Main.kt<br>public fun String.minus(str: String): String defined in h in file minusOperator.before.Main.kt
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

package h

interface H

fun f(h: H?) {
    h <caret>- "other"
}


fun String.minus(str: String) = ""

fun String.minus(i: Integer) = ""
/* IGNORE_FIR */