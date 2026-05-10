// ISSUE: KT-85300
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

fun String.foo() {}

fun String.bar() {}
fun Int.bar() {}

fun test() {
    true.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
    true.<!NONE_APPLICABLE!>bar<!>()
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration */
