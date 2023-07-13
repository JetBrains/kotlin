// FILE: main.kts
fun foo<caret>(): a.b.c.dependency.Foo = t

// FILE: dependency.kt
package a.b.c.dependency

class Foo {
    class Nested
}
