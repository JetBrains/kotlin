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

        fun foo() {
            my.simple.name<caret>.SuperClass.check()
            Child.Foo.check()
        }

        companion object {
            fun check() {}
        }
    }
}