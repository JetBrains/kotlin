// WITH_STDLIB
// MODULE: m1
// FILE: main.kt
typealias MyString = String

fun test(value: <expr>MyString</expr>) {}

// MODULE: m2
// FILE: unrelated.kt
class MyString

fun foo() {
    <caret_restoreAt>
}
