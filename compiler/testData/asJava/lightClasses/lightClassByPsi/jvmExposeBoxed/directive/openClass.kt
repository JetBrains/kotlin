// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IC(val i: Int)

interface Test {
    fun test(p: IC): IC
    val foo: IC?
}

open class TestClass1 : Test {
    override fun test(p: IC): IC {
        return p
    }
    override val foo: IC? get() = IC(1)
}