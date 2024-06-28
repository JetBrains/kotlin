// FILE: main.kt
package test

fun usage() {
    <expr>dependency.foo += 20</expr>
}

// FILE: dependency.kt
package dependency

val foo: Foo = Foo()

class Foo {
    operator fun plusAssign(i: Int) {}
}