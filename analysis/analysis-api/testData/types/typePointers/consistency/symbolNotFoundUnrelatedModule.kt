// COMPILATION_ERRORS
// MODULE: m1
// FILE: main.kt

fun test(foo: <expr>Foo<String></expr>) {}
// MODULE: m2
// FILE: unrelated.kt
fun foo() {
    <caret_restoreAt>
}
