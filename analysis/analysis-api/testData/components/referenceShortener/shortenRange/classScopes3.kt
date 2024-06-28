// FILE: main.kt
package my.simple.name

class ClassFarAway {
    companion object {
        fun check() {}
    }
}

open class SuperClass {
    companion object {
        fun check() {}
    }
}

class Child: SuperClass {
    class Foo constructor() {
        constructor(i: Int) : this()

        fun foo() {
            <expr>my.simple.name.ClassFarAway.check()</expr>
        }

        companion object {
            fun check() {}
        }
    }
}