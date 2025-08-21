// WITH_STDLIB
// MODULE: m1
// FILE: main.kt
typealias MyList<T> = List<T>
typealias MyString = String

fun test(value: <expr>MyList<MyString></expr>) {}

// MODULE: m2
// FILE: unrelated.kt
fun foo() {
    <caret_restoreAt>
}
