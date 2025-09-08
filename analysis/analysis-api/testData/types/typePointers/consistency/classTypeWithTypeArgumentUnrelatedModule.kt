// MODULE: m1
// FILE: main.kt
interface MyCollection<T>
interface Foo

class Usage(val list: <expr>MyCollection<Foo></expr>)

// MODULE: m2
// FILE: unrelated.kt
fun foo() {
    <caret_restoreAt>
}
