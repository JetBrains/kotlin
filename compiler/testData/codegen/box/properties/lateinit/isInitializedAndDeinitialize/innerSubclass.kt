// TARGET_BACKEND: JVM
// LANGUAGE_VERSION: 1.2
// WITH_RUNTIME

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
