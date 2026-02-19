// MODULE: m1
// FILE: main.kt
interface Foo

class Usage(val list: <expr>Foo</expr>)

// MODULE: m2
// FILE: unrelated.kt
fun foo() {
    <caret_restoreAt>
}
