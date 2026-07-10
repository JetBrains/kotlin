// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocksAndExtensions

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

class WithCompanionBlock(val id: Int) {
    companion {
        val defaultId: Int = 0
        const val TAG: String = "WithCompanionBlock"
        fun create(): WithCompanionBlock = WithCompanionBlock(defaultId)
        fun describe(): String = "[$TAG]"
    }
    fun instanceFun(): String = "instance:$id"
}

class WithBothCompanions(val x: Int) {
    companion {
        val blockProp: String = "from-block"
        fun blockFun(): String = blockProp
    }
    companion object Named {
        const val OBJ_CONST = "from-object"
        fun objFun(): String = OBJ_CONST
    }
}

open class Base {
    companion {
        val baseProp: String = "base"
        fun baseFun(): String = baseProp
    }
}

class Derived : Base() {
    companion {
        val derivedProp: String = "derived"
        fun derivedFun(): String = derivedProp
    }
}

companion fun WithCompanionBlock.extensionFun(): String = "ext"
companion val WithCompanionBlock.extensionProp: String get() = "ext-prop"

fun box(): String {
    // Companion block members appear in staticFunctions / staticProperties, NOT memberFunctions
    val staticFns = WithCompanionBlock::class.staticFunctions.map { it.name }.toSet()
    assertTrue("create" in staticFns, "create should be in staticFunctions: $staticFns")
    assertTrue("describe" in staticFns, "describe should be in staticFunctions: $staticFns")
    assertFalse("instanceFun" in staticFns, "instanceFun should NOT be in staticFunctions")

    val staticProps = WithCompanionBlock::class.staticProperties.map { it.name }.toSet()
    assertTrue("defaultId" in staticProps || "TAG" in staticProps,
        "Companion block properties should be in staticProperties: $staticProps")

    // Static functions have NO INSTANCE parameter (they are static)
    val createFn = WithCompanionBlock::class.staticFunctions.first { it.name == "create" }
    assertTrue(createFn.parameters.none { it.kind == KParameter.Kind.INSTANCE },
        "Static function should have no INSTANCE parameter")
    assertEquals(emptyList(), createFn.valueParameters)

    // Static function is callable directly (no instance needed)
    val instance = createFn.call()
    assertEquals(WithCompanionBlock(0), (instance as? WithCompanionBlock)?.let { it.id == 0 }.let { instance })

    // Static properties have no INSTANCE parameter
    val defaultIdProp = WithCompanionBlock::class.staticProperties.first { it.name == "defaultId" }
    assertEquals(emptyList(), defaultIdProp.parameters)
    assertEquals(0, defaultIdProp.call())

    // Companion block + companion object coexistence
    val bothStaticFns = WithBothCompanions::class.staticFunctions.map { it.name }.toSet()
    assertTrue("blockFun" in bothStaticFns, "blockFun from companion block: $bothStaticFns")
    val bothMemberFns = WithBothCompanions::class.memberFunctions.map { it.name }.toSet()
    // objFun is in companion object, accessible differently
    // The companion object has its own KClass
    assertNotNull(WithBothCompanions::class.companionObject)

    // Inheritance: Derived sees its own static members, not Base's
    val derivedStaticFns = Derived::class.staticFunctions.map { it.name }.toSet()
    assertTrue("derivedFun" in derivedStaticFns, "derivedFun should be in Derived staticFunctions")
    assertFalse("baseFun" in derivedStaticFns, "baseFun should NOT be inherited in staticFunctions")

    // members() includes both static and instance members
    val allMembers = WithCompanionBlock::class.members.map { it.name }.toSet()
    assertTrue("create" in allMembers)
    assertTrue("instanceFun" in allMembers)
    assertTrue("defaultId" in allMembers || "TAG" in allMembers)

    return "OK"
}
