// DIAGNOSTICS: -ERROR_SUPPRESSION
// FILE: foo/A.kt
<!CLASHED_FILES_IN_CASE_INSENSITIVE_FS!>package foo

fun test() = 1<!>

// FILE: bar/a.kt
<!CLASHED_FILES_IN_CASE_INSENSITIVE_FS!>package foo

fun bar() = 2<!>
