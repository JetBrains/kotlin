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

// DECLARATIONS_NO_LIGHT_ELEMENTS: Test.class[foo;test], TestClass1.class[foo;test]
// LIGHT_ELEMENTS_NO_DECLARATION: IC.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Test.class[getFoo-qjS0p_s;test-Eh1mVAw], TestClass1.class[getFoo-qjS0p_s;test-Eh1mVAw]