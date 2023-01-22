// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlin.Self

@Self
open class Foo<T> {
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

fun box(): String {
    val foo = Foo<Int>()
    return foo.box()
}