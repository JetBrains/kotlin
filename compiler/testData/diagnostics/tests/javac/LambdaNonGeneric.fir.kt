// RENDER_DIAGNOSTICS_FULL_TEXT
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
// LANGUAGE: -ForbidLambdaParameterWithMissingDependencyType
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
    foo { <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>_<!>, _ -> }
    foo { <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>some<!>, str -> }
    foo { <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>some<!>, _ -> <!MISSING_DEPENDENCY_CLASS!>some<!>.toString() }
    foo { some: <!UNRESOLVED_REFERENCE!>Some<!>, _ -> }

    bar <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>{ }<!>
    bar { <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>_<!> -> }
    bar { <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>it<!> -> }
    bar <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>{ <!MISSING_DEPENDENCY_CLASS!>it<!>.toString() }<!>
    bar { <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>some<!> -> <!MISSING_DEPENDENCY_CLASS!>some<!>.toString() }
}
