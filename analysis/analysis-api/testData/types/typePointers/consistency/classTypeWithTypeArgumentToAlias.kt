// MODULE: m1
// FILE: main.kt
interface Foo<T>

class Usage(val list: <expr>Foo<String></expr>)

// MODULE: m2
// FILE: unrelated.kt
typealias Foo = String
fun foo() {
    <caret_restoreAt>
}
