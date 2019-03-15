// WITH_RUNTIME
package my.simple.name

open class SuperClass {
    companion object {
        fun check() {}
    }
}

class Child : SuperClass() {
    class Foo constructor() {
        constructor(i: Int) : this()
        fun check() {}

        fun foo() {
            my.simple.name.SuperClass.check()
            Child<caret>.Foo.check()
        }

        companion object {
            fun check() {}
        }
    }
}