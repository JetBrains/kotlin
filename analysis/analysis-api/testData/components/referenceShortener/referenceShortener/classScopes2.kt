// FILE: main.kt
package my.simple.name

open class SuperClass {
    companion object {
        fun check() {}
    }
}

class Child: SuperClass {
    class Foo constructor() {
        constructor(i: Int) : this()

        fun foo() {
            <expr>Child.Foo.check()</expr>
        }

        companion object {
            fun check() {}
        }
    }
}