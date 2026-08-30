// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class IC(val i: Int)

interface Test {
    fun test(p: IC): IC
    fun finalTest(p: IC): IC
    val foo: IC?
    var finalFoo: IC?
}

open class TestClass1 : Test {
    override fun test(p: IC): IC {
        return p
    }

    final override fun finalTest(p: IC): IC {
        return p
    }

    override val foo: IC? get() = IC(1)

    final override var finalFoo: IC? = IC(1)
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Test.class[finalFoo;finalTest;foo;test], TestClass1.class[foo;test]
// LIGHT_ELEMENTS_NO_DECLARATION: IC.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Test.class[finalTest-Eh1mVAw;getFinalFoo-qjS0p_s;getFoo-qjS0p_s;setFinalFoo-6iVyXxs;test-Eh1mVAw], TestClass1.class[finalTest-Eh1mVAw;getFinalFoo-qjS0p_s;getFoo-qjS0p_s;setFinalFoo-6iVyXxs;test-Eh1mVAw]
