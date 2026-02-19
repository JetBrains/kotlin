// FILE: main.kt
package test

fun usage(foo: <expr>dependency.Foo</expr>.Bar) {}

// FILE: dependency.kt
package dependency

class Foo {
    class Bar
}