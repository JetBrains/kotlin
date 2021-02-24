// !LANGUAGE: +NoConstantValueAttributeForNonConstVals +JvmFieldInInterface
// TARGET_BACKEND: JVM
// WITH_RUNTIME

import kotlin.test.assertEquals

class C {
    val testClassVal = 100

    @JvmField
    val testJvmFieldVal = 105

    companion object {
        val testCompanionObjectVal = 110

        @JvmStatic
        val testJvmStaticCompanionObjectVal = 120

        @JvmField
        val testJvmFieldCompanionObjectVal = 130
    }
}


interface IFoo {
    companion object {
        val testInterfaceCompanionObjectVal = 200
    }
}


interface IBar {
    companion object {
        @JvmField
        val testJvmFieldInInterfaceCompanionObject = 210
    }
}


object Obj {
    val testObjectVal = 300

    @JvmStatic
    val testJvmStaticObjectVal = 310

    @JvmField
    val testJvmFieldObjectVal = 320
}


val testTopLevelVal = 400

fun box(): String {
    assertEquals(100, C().testClassVal)
    assertEquals(105, C().testJvmFieldVal)
    assertEquals(110, C.testCompanionObjectVal)
    assertEquals(120, C.testJvmStaticCompanionObjectVal)
    assertEquals(130, C.testJvmFieldCompanionObjectVal)
    assertEquals(200, IFoo.testInterfaceCompanionObjectVal)
    assertEquals(210, IBar.testJvmFieldInInterfaceCompanionObject)
    assertEquals(300, Obj.testObjectVal)
    assertEquals(310, Obj.testJvmStaticObjectVal)
    assertEquals(320, Obj.testJvmFieldObjectVal)
    assertEquals(400, testTopLevelVal)

    return "OK"
}