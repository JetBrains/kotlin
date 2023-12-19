// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
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
    foo { <!MISSING_DEPENDENCY_CLASS!>_<!>, _ -> }
    foo { <!MISSING_DEPENDENCY_CLASS!>some<!>, str -> }
    foo { <!MISSING_DEPENDENCY_CLASS!>some<!>, _ -> <!MISSING_DEPENDENCY_CLASS!>some<!>.toString() }
    foo { some: <!UNRESOLVED_REFERENCE!>Some<!>, _ -> }

    bar <!MISSING_DEPENDENCY_CLASS!>{ }<!>
    bar { <!MISSING_DEPENDENCY_CLASS!>_<!> -> }
    bar { <!MISSING_DEPENDENCY_CLASS!>it<!> -> }
    bar <!MISSING_DEPENDENCY_CLASS!>{ <!MISSING_DEPENDENCY_CLASS!>it<!>.toString() }<!>
    bar { <!MISSING_DEPENDENCY_CLASS!>some<!> -> <!MISSING_DEPENDENCY_CLASS!>some<!>.toString() }
}
