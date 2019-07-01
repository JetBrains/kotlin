// WITH_RUNTIME
package my.simple.name

class Outer {
    class Foo constructor() {
        constructor(i: Int) : this()

        companion object {
            fun check() {
                val a = Outer<caret>.Foo(1)
                val b = Outer.Foo()
            }
        }
    }
}