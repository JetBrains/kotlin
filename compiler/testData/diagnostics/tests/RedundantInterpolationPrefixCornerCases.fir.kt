// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiDollarInterpolation
// WITH_EXPERIMENTAL_CHECKERS
// DIAGNOSTICS: -warnings +REDUNDANT_INTERPOLATION_PREFIX

fun test() {
    "foo\\\$bar"
    $$"foo\\$bar"
    "foo\\$<!UNRESOLVED_REFERENCE!>bar<!>"
    "\$%"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"$%"<!>
    "$%"
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
