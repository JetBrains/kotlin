// RUN_PIPELINE_TILL: FRONTEND

package p

enum class MyLevel { ERROR }

@Deprecated("", level = <!ARGUMENT_TYPE_MISMATCH!>MyLevel.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>ERROR<!><!>)
fun wrongEnum() {}

enum class DeprecationLevel { ERROR }

@Deprecated("", level = <!ARGUMENT_TYPE_MISMATCH!>DeprecationLevel.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>ERROR<!><!>)
fun wrongDeprecationLevel() {}

@Deprecated("", ReplaceWith(""), <!ARGUMENT_TYPE_MISMATCH!>p.DeprecationLevel.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>ERROR<!><!>)
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
