// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-87025

import kotlin.DeprecationLevel.WARNING as ERROR

@Deprecated("", level = ERROR)
fun aliasedLevel() {}

@Deprecated("", level = DeprecationLevel.WARNING)
fun plainLevel() {}

fun use() {
    <!DEPRECATION!>aliasedLevel<!>()
    <!DEPRECATION!>plainLevel<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
