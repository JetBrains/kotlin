// COMPILATION_ERRORS
// WITH_STDLIB
// MODULE: m1
// FILE: main.kt

fun test(foo: <expr>List<Foo></expr>) {}
// MODULE: m2
// FILE: unrelated.kt
class Foo

fun foo() {
    <caret_restoreAt>
}
