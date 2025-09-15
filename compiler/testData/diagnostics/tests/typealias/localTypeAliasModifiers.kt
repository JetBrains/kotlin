// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun outer() {
    <!WRONG_MODIFIER_TARGET!>companion<!> typealias TestLocal = Any
}

/* GENERATED_FIR_TAGS: functionDeclaration, typeAliasDeclaration */
