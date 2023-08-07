// FILE: main.kt
package test

fun usage() {
    <expr>dependency.foo()</expr>
}

// FILE: dependency.kt
package dependency

var foo: Foo = Foo()

class Foo {
    operator fun invoke() {}
}