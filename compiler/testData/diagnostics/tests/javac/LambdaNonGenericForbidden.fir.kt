// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
// LANGUAGE: +ForbidLambdaParameterWithMissingDependencyType
// ISSUE: KT-64266

// MODULE: m1
// FILE: m1.kt

class Some

// MODULE: m2(m1)
// FILE: m2.kt

fun foo(f: (Some, String) -> Unit) {}
fun bar(f: (Some) -> Unit) {}

// MODULE: m3(m2)
// FILE: m3.kt

fun test() {
    <!MISSING_DEPENDENCY_CLASS!>foo<!> { <!MISSING_DEPENDENCY_CLASS!>_<!>, _ -> }
    <!MISSING_DEPENDENCY_CLASS!>foo<!> { <!MISSING_DEPENDENCY_CLASS!>some<!>, str -> }
    <!MISSING_DEPENDENCY_CLASS!>foo<!> { <!MISSING_DEPENDENCY_CLASS!>some<!>, _ -> <!MISSING_DEPENDENCY_CLASS!>some<!>.toString() }
    <!MISSING_DEPENDENCY_CLASS!>foo<!> { some: <!UNRESOLVED_REFERENCE!>Some<!>, _ -> }

    <!MISSING_DEPENDENCY_CLASS!>bar<!> <!MISSING_DEPENDENCY_CLASS!>{ }<!>
    <!MISSING_DEPENDENCY_CLASS!>bar<!> { <!MISSING_DEPENDENCY_CLASS!>_<!> -> }
    <!MISSING_DEPENDENCY_CLASS!>bar<!> { <!MISSING_DEPENDENCY_CLASS!>it<!> -> }
    <!MISSING_DEPENDENCY_CLASS!>bar<!> <!MISSING_DEPENDENCY_CLASS!>{ <!MISSING_DEPENDENCY_CLASS!>it<!>.toString() }<!>
    <!MISSING_DEPENDENCY_CLASS!>bar<!> { <!MISSING_DEPENDENCY_CLASS!>some<!> -> <!MISSING_DEPENDENCY_CLASS!>some<!>.toString() }
}
