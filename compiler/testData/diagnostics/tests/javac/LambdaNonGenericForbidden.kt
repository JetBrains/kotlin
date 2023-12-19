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
    foo { _, _ -> }
    foo { some, str -> }
    foo { some, _ -> some.toString() }
    foo { some: <!UNRESOLVED_REFERENCE!>Some<!>, _ -> }

    bar { }
    bar { _ -> }
    bar { it -> }
    bar { it.toString() }
    bar { some -> some.toString() }
}
