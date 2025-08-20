// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80255

external fun foo(f: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int<!>.() -> Int)

/* GENERATED_FIR_TAGS: external, functionDeclaration */
