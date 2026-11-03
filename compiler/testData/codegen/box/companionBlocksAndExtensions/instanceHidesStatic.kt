// LANGUAGE: +CompanionBlocks

import kotlin.test.assertEquals

open class Base {
    companion {
        // To be hidden by regular properties
        var companionVar = "Base::companionVar"
        val companionDelegatedVal by lazy { "Base::companionDelegatedVal" }

        // To be hidden by generated interface delegation members
        fun interfaceFun() = "Base::interfaceFun"
        val interfaceVal: Int get() = 1

        // To be hidden by generated data class members
        fun component1(): Int = 2
        fun copy(x: Int): Any = x
    }
}

interface Interface {
    fun interfaceFun(): String
    val interfaceVal: Int
}

class Regular : Base() {
    var companionVar = "Regular::companionVar"
    val companionDelegatedVal by lazy { "Regular::companionDelegatedVal" }
}

class Delegating(delegate: Interface) : Base(), Interface by delegate

data class Data(val x: Int) : Base()

fun box(): String {
    assertEquals("Base::companionVar", Base.companionVar)
    assertEquals("Base::companionDelegatedVal", Base.companionDelegatedVal)
    assertEquals(1, Base.interfaceVal)
    assertEquals(2, Base.component1())
    assertEquals(3, Base.copy(3))

    assertEquals("Regular::companionVar", Regular().companionVar)
    assertEquals("Regular::companionDelegatedVal", Regular().companionDelegatedVal)

    val delegating = Delegating(object : Interface {
        override fun interfaceFun() = "delegated interface fun"
        override val interfaceVal: Int get() = 123
    })
    assertEquals("delegated interface fun", delegating.interfaceFun())
    assertEquals(123, delegating.interfaceVal)

    assertEquals(Data(42), Data(0).copy(x = 42))

    return "OK"
}
