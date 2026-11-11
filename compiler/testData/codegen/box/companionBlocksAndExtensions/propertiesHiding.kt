// LANGUAGE: +CompanionBlocks

import kotlin.test.assertEquals

open class Base {
    companion {
        val valProp: Int = 1
        var varProp: Int = 2
        var propWithPrivateSetter: Int
            get() = 3
            private set(value) {}
    }
}

class HideSameTypeVal : Base() {
    companion {
        val valProp: Int = 11
        val varProp: Int = 22
        val propWithPrivateSetter: Int = 33
    }
}

class HideSameTypeVar : Base() {
    companion {
        var valProp: Int = 11
        var varProp: Int = 22
        var propWithPrivateSetter: Int = 33
    }
}

class HideSameTypePrivateSet : Base() {
    companion {
        var valProp: Int = 11; private set
        var varProp: Int = 22; private set
        var propWithPrivateSetter: Int = 33; private set
    }
}

class HideDifferentTypeVal : Base() {
    companion {
        val valProp: String = "a"
        val varProp: String = "b"
        val propWithPrivateSetter: String = "c"
    }
}

class HideDifferentTypeVar : Base() {
    companion {
        var valProp: String = "a"
        var varProp: String = "b"
        var propWithPrivateSetter: String = "c"
    }
}

class HideDifferentTypePrivateSet : Base() {
    companion {
        var valProp: String = "a"; private set
        var varProp: String = "b"; private set
        var propWithPrivateSetter: String = "c"; private set
    }
}

fun box(): String {
    assertEquals(1, Base.valProp)
    assertEquals(2, Base.varProp)
    assertEquals(3, Base.propWithPrivateSetter)

    assertEquals(11, HideSameTypeVal.valProp)
    assertEquals(22, HideSameTypeVal.varProp)
    assertEquals(33, HideSameTypeVal.propWithPrivateSetter)

    assertEquals(11, HideSameTypeVar.valProp)
    assertEquals(22, HideSameTypeVar.varProp)
    assertEquals(33, HideSameTypeVar.propWithPrivateSetter)

    assertEquals(11, HideSameTypePrivateSet.valProp)
    assertEquals(22, HideSameTypePrivateSet.varProp)
    assertEquals(33, HideSameTypePrivateSet.propWithPrivateSetter)

    assertEquals("a", HideDifferentTypeVal.valProp)
    assertEquals("b", HideDifferentTypeVal.varProp)
    assertEquals("c", HideDifferentTypeVal.propWithPrivateSetter)

    assertEquals("a", HideDifferentTypeVar.valProp)
    assertEquals("b", HideDifferentTypeVar.varProp)
    assertEquals("c", HideDifferentTypeVar.propWithPrivateSetter)

    assertEquals("a", HideDifferentTypePrivateSet.valProp)
    assertEquals("b", HideDifferentTypePrivateSet.varProp)
    assertEquals("c", HideDifferentTypePrivateSet.propWithPrivateSetter)

    return "OK"
}
