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

// DECLARATIONS_NO_LIGHT_ELEMENTS: Test.class[test]
// LIGHT_ELEMENTS_NO_DECLARATION: IC.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Test.class[foo-Eh1mVAw;getBar-qjS0p_s;test-Eh1mVAw], TestClass.class[test-Eh1mVAw]