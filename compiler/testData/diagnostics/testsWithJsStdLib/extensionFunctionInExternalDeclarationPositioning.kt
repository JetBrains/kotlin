// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80255

external fun foo(<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>f: Int.() -> Int<!>)

/* GENERATED_FIR_TAGS: external, functionDeclaration */
