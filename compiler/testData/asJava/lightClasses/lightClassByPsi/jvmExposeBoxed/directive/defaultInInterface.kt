// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IC(val i: Int)

interface Test {
    fun test(p: IC): IC = foo(p)
    private val bar: IC? get() = null
    private fun foo(o: IC): IC = o
}

class TestClass : Test {
    override fun test(p: IC): IC {
        return super.test(p)
    }
}