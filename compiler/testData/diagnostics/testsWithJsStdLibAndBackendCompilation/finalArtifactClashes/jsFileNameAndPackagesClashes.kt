// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FILE: A.kt
<!CLASHED_FILES_IN_CASE_INSENSITIVE_FS!>package foo

fun test() = 1<!>

// FILE: B.kt
<!CLASHED_FILES_IN_CASE_INSENSITIVE_FS!>@file:JsFileName("a")
package foo

fun bar() = 2<!>
