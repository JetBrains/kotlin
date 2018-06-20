// IGNORE_BACKEND: JS_IR
interface IFoo {
    fun foo(): String
}

interface IBar {
    fun bar(): String
}

abstract class Base(val x: IFoo)

enum class Test : IFoo, IBar {
    FOO {
        // FOO referenced from inner class constructor with uninitialized 'this'
        inner class Inner : Base(FOO)

        val z = Inner()

        override fun foo() = "OK"

        override fun bar() = z.x.foo()
    }
}

fun box() = Test.FOO.bar()