// TARGET_BACKEND: JVM
// WITH_STDLIB

object Test {
    @JvmStatic
    fun foo() = bar()

    @JvmStatic
    fun foo1() {
        bar()
        val baz = bar()
        return baz
    }

    @JvmStatic
    fun bar() {}
}

// 0 GETSTATIC Test.INSTANCE : LTest;
// 0 POP
