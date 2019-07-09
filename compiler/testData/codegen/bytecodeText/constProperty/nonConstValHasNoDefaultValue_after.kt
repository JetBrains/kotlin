// !LANGUAGE: +NoConstantValueAttributeForNonConstVals +JvmFieldInInterface
// IGNORE_BACKEND: JVM_IR

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


// 0 final I testClassVal = 100
// 1 final I testClassVal
// 0 final I testJvmFieldVal = 105
// 1 final I testJvmFieldVal
// 0 final static I testCompanionObjectVal = 110
// 1 final static I testCompanionObjectVal
// 0 final static I testJvmStaticCompanionObjectVal = 120
// 1 final static I testJvmStaticCompanionObjectVal
// 0 final static I testJvmFieldCompanionObjectVal = 130
// 1 final static I testJvmFieldCompanionObjectVal
// 0 final static I testInterfaceCompanionObjectVal = 200
// 1 final static I testInterfaceCompanionObjectVal
// 0 final static I testJvmFieldInInterfaceCompanionObject = 210
// 1 final static I testJvmFieldInInterfaceCompanionObject
// 0 final static I testObjectVal = 300
// 1 final static I testObjectVal
// 0 final static I testJvmStaticObjectVal = 310
// 1 final static I testJvmStaticObjectVal
// 0 final static I testJvmFieldObjectVal = 320
// 1 final static I testJvmFieldObjectVal
// 0 final static I testTopLevelVal = 400
// 1 final static I testTopLevelVal
