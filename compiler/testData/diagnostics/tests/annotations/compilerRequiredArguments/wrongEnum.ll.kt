// LL_FIR_DIVERGENCE
// KT-88962
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND

package p

enum class MyLevel { ERROR }

@Deprecated("", level = <!ARGUMENT_TYPE_MISMATCH!>MyLevel.ERROR<!>)
fun wrongEnum() {}

enum class DeprecationLevel { ERROR }

@Deprecated("", level = <!ARGUMENT_TYPE_MISMATCH!>DeprecationLevel.ERROR<!>)
fun wrongDeprecationLevel() {}

@Deprecated("", ReplaceWith(""), <!ARGUMENT_TYPE_MISMATCH!>p.DeprecationLevel.ERROR<!>)
fun wrongDeprecationLevel2() {}

@Deprecated("", ReplaceWith(""), kotlin.DeprecationLevel.ERROR)
fun rightDeprecationLevel() {}

fun use() {
    <!DEPRECATION_ERROR!>wrongEnum<!>()
    <!DEPRECATION_ERROR!>wrongDeprecationLevel<!>()
    <!DEPRECATION_ERROR!>wrongDeprecationLevel2<!>()
    <!DEPRECATION_ERROR!>rightDeprecationLevel<!>()
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, stringLiteral */
