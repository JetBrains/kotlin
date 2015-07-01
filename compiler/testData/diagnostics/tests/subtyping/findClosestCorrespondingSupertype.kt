// !DIAGNOSTICS: -UNUSED_VARIABLE

interface X<T>
interface A: X<String>
interface B : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A, X<Int><!>

fun foo(x: B) {
    // Checks that when checking subtypes we search closes corresponding constructor (e.g. with BFS)
    val y: X<Int> = x
}
