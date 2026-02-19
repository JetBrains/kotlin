// FILE: main.kt
package test

fun usage() {
    <expr>dependency.foo += 20</expr>
}

// FILE: dependency.kt
package dependency

var foo: Foo = Foo()

class Foo {
    operator fun plus(i: Int): Foo = this
}