// TARGET_BACKEND: JVM
// JVM_DEFAULT_MODE: enable

interface Foo<T> {
    fun test(p: T): String
}

interface Foo2 : Foo<String> {
    override fun test(p: String): String = p
}

interface Foo3 : Foo<String>, Foo2

class Base : Foo3 {
    fun t(): String = super.test("OK")

    override fun test(p: String): String = "Fail"
}

fun box(): String = Base().t()
