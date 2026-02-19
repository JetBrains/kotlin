// WITH_STDLIB

import kotlin.test.*

open class Foo {
    lateinit var bar: String

    fun test(): String {
        return InnerSubclass().testInner()
    }

    inner class InnerSubclass : Foo() {
        fun testInner(): String {
            // This is access to InnerSubclass.bar which is inherited from Foo.bar
            if (this::bar.isInitialized) return "Fail"
            return "OK"
        }
    }
}

fun box(): String {
    return Foo().test()
}
