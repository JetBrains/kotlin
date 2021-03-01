// !DIAGNOSTICS: -UNUSED_VARIABLE

interface X<T>
interface A: X<String>
interface B : A, X<Int>

fun foo(x: B) {
    // Checks that when checking subtypes we search closes corresponding constructor (e.g. with BFS)
    // may be an InitializerTypeMismatchChecker bug
    val y: X<Int> = <!INITIALIZER_TYPE_MISMATCH!>x<!>
}
