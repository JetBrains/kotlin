// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

fun test() {
    kotlin.<!NONE_APPLICABLE!>context<!>() { }
}
