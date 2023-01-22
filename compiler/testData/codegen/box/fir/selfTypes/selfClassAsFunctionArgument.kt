// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlin.Self

@Self
class Foo<T> {
    public val bar = 1

    fun test(): Self = this as Self

    fun box(): String {
        val testSelf = test()
        if (testSelf.bar == this.bar)
            return "OK"
        else
            return "ERROR"
    }
}

class Bar<T> {
    fun f(foo: Foo<T, *>): String = foo.box()
}

fun box(): String {
    val foo = Foo<Int>()
    val bar = Bar<Int>()
    return bar.f(foo)
}