// LANGUAGE: +CompanionBlocks
// DISABLE_IR_VISIBILITY_CHECKS: ANY
//   ^^^ KT-91454

// MODULE: a
// FILE: a.kt

open class Base {
    companion {
        var publicSet: Int = 1
        var internalSet: Int = 2; internal set
    }
}

class HideSameTypeSameModule : Base() {
    companion {
        var publicSet: Int = 3
        var internalSet: Int = 4
    }
}

class HideDifferentTypeSameModule : Base() {
    companion {
        var publicSet: String = "a"
        var internalSet: String = "b"
    }
}

class HideSameTypeSameModuleInternalSet : Base() {
    companion {
        var publicSet: Int = 3; internal set
        var internalSet: Int = 4; internal set
    }
}

class HideDifferentTypeSameModuleInternalSet : Base() {
    companion {
        var publicSet: String = "a"; internal set
        var internalSet: String = "b"; internal set
    }
}

// MODULE: main(a)
// FILE: main.kt

import kotlin.test.assertEquals

class HideSameTypeOtherModule : Base() {
    companion {
        var publicSet: Int = 3
        var internalSet: Int = 4
    }
}

class HideDifferentTypeOtherModule : Base() {
    companion {
        var publicSet: String = "a"
        var internalSet: String = "b"
    }
}

class HideSameTypeOtherModuleInternalSet : Base() {
    companion {
        var publicSet: Int = 3; internal set
        var internalSet: Int = 4; internal set
    }
}

class HideDifferentTypeOtherModuleInternalSet : Base() {
    companion {
        var publicSet: String = "a"; internal set
        var internalSet: String = "b"; internal set
    }
}

fun box(): String {
    assertEquals(1, Base.publicSet)
    assertEquals(2, Base.internalSet)

    assertEquals(3, HideSameTypeSameModule.publicSet)
    assertEquals(4, HideSameTypeSameModule.internalSet)

    assertEquals("a", HideDifferentTypeSameModule.publicSet)
    assertEquals("b", HideDifferentTypeSameModule.internalSet)

    assertEquals(3, HideSameTypeSameModuleInternalSet.publicSet)
    assertEquals(4, HideSameTypeSameModuleInternalSet.internalSet)

    assertEquals("a", HideDifferentTypeSameModuleInternalSet.publicSet)
    assertEquals("b", HideDifferentTypeSameModuleInternalSet.internalSet)

    assertEquals(3, HideSameTypeOtherModule.publicSet)
    assertEquals(4, HideSameTypeOtherModule.internalSet)

    assertEquals("a", HideDifferentTypeOtherModule.publicSet)
    assertEquals("b", HideDifferentTypeOtherModule.internalSet)

    assertEquals(3, HideSameTypeOtherModuleInternalSet.publicSet)
    assertEquals(4, HideSameTypeOtherModuleInternalSet.internalSet)

    assertEquals("a", HideDifferentTypeOtherModuleInternalSet.publicSet)
    assertEquals("b", HideDifferentTypeOtherModuleInternalSet.internalSet)

    return "OK"
}
