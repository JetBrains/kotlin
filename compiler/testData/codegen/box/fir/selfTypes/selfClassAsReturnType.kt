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
    fun foo(): Foo<T> = Foo<T>()
}

fun box(): String {
    val bar = Bar<Int>()
    return bar.foo().box()
}