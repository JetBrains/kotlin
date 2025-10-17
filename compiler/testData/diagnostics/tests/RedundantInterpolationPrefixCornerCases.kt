// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiDollarInterpolation
// WITH_EXPERIMENTAL_CHECKERS
// DIAGNOSTICS: -warnings +REDUNDANT_INTERPOLATION_PREFIX

fun test() {
    "foo\\\$bar"
    <!UNSUPPORTED_FEATURE!>$$"foo\\$bar"<!>
    "foo\\$bar"
    "\$%"
    <!UNSUPPORTED_FEATURE!>$$"$%"<!>
    "$%"
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
