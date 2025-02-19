// WITH_STDLIB
// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

open class Foo {
    lateinit var bar: String

    private lateinit var baz: String

    fun test(): String {
        val isBarInitialized: () -> Boolean = { this::bar.isInitialized }
        if (isBarInitialized()) return "Fail 1"
        bar = "bar"
        if (!isBarInitialized()) return "Fail 2"
        baz = "baz"
        return InnerSubclass().testInner()
    }

    inner class InnerSubclass : Foo() {
        fun testInner(): String {
            // This is access to InnerSubclass.bar which is inherited from Foo.bar
            if (this::bar.isInitialized) return "Fail 3"
            bar = "OK"
            if (!this::bar.isInitialized) return "Fail 4"

            // This is access to Foo.bar declared lexically above
            if (!this@Foo::bar.isInitialized) return "Fail 5"
            return "OK"
        }
    }
}

fun box(): String {
    return Foo().test()
}
