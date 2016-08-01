package ldifferentTypes

fun main(args: Array<String>) {
    // EXPRESSION: str_DebugLabel
    // RESULT: "str": Ljava/lang/String;
    // DEBUG_LABEL: str = str
    val str = "str"
    // EXPRESSION: strArray_DebugLabel
    // RESULT: instance of java.lang.String[1] (id=ID): [Ljava/lang/String;
    // DEBUG_LABEL: strArray = strArray
    val strArray = arrayOf("str")

    // EXPRESSION: primitiveArray_DebugLabel
    // RESULT: instance of java.lang.Integer[1] (id=ID): [Ljava/lang/Integer;
    // DEBUG_LABEL: primitiveArray = primitiveArray
    val primitiveArray = arrayOf(1)

    // EXPRESSION: localObj_DebugLabel
    // RESULT: instance of ldifferentTypes.LdifferentTypesKt$main$localObj$1(id=ID): LldifferentTypes/LdifferentTypesKt$main$localObj$1;
    // EXPRESSION: localObj_DebugLabel.test()
    // RESULT: Unresolved reference: test
    // DEBUG_LABEL: localObj = localObj
    val localObj = object { fun test() = 1 }
    // EXPRESSION: localObjArray_DebugLabel
    // RESULT: instance of ldifferentTypes.LdifferentTypesKt$main$localObj$1[1] (id=ID): [LldifferentTypes/LdifferentTypesKt$main$localObj$1;
    // EXPRESSION: localObjArray_DebugLabel[0].test()
    // RESULT: Unresolved reference: test
    // DEBUG_LABEL: localObjArray = localObjArray
    val localObjArray = arrayOf(localObj)

    class LocalClass {
        fun test() = 1
    }

    // EXPRESSION: localClass_DebugLabel
    // RESULT: instance of ldifferentTypes.LdifferentTypesKt$main$localClass$1(id=ID): LldifferentTypes/LdifferentTypesKt$main$localClass$1;
    // EXPRESSION: localClass_DebugLabel.test()
    // RESULT: Unresolved reference: test
    // DEBUG_LABEL: localClass = localClass
    val localClass = object { fun test() = 1 }
    // EXPRESSION: localClassArray_DebugLabel
    // RESULT: instance of ldifferentTypes.LdifferentTypesKt$main$localClass$1[1] (id=ID): [LldifferentTypes/LdifferentTypesKt$main$localClass$1;
    // EXPRESSION: localClassArray_DebugLabel[0].test()
    // RESULT: Unresolved reference: test
    // DEBUG_LABEL: localClassArray = localClassArray
    val localClassArray = arrayOf(localClass)

    // EXPRESSION: aClass_DebugLabel
    // RESULT: instance of ldifferentTypes.A(id=ID): LldifferentTypes/A;
    // EXPRESSION: aClass_DebugLabel.test()
    // RESULT: 1: I
    // DEBUG_LABEL: aClass = aClass
    val aClass = A()
    // EXPRESSION: aClassArray_DebugLabel
    // RESULT: instance of ldifferentTypes.A[1] (id=ID): [LldifferentTypes/A;
    // EXPRESSION: aClassArray_DebugLabel[0].test()
    // RESULT: 1: I
    // DEBUG_LABEL: aClassArray = aClassArray
    val aClassArray = arrayOf(aClass)

    // EXPRESSION: innerClass_DebugLabel
    // RESULT: instance of ldifferentTypes.A$B(id=ID): LldifferentTypes/A$B;
    // EXPRESSION: innerClass_DebugLabel.test()
    // RESULT: 1: I
    // DEBUG_LABEL: innerClass = innerClass
    val innerClass = A().B()
    // EXPRESSION: innerClassArray_DebugLabel
    // RESULT: instance of ldifferentTypes.A$B[1] (id=ID): [LldifferentTypes/A$B;
    // EXPRESSION: innerClassArray_DebugLabel[0].test()
    // RESULT: 1: I
    // DEBUG_LABEL: innerClassArray = innerClassArray
    val innerClassArray = arrayOf(innerClass)

    // EXPRESSION: nestedClass_DebugLabel
    // RESULT: instance of ldifferentTypes.A$C(id=ID): LldifferentTypes/A$C;
    // EXPRESSION: nestedClass_DebugLabel.test()
    // RESULT: 1: I
    // DEBUG_LABEL: nestedClass = nestedClass
    val nestedClass = A.C()
    // EXPRESSION: nestedClassArray_DebugLabel
    // RESULT: instance of ldifferentTypes.A$C[1] (id=ID): [LldifferentTypes/A$C;
    // EXPRESSION: nestedClassArray_DebugLabel[0].test()
    // RESULT: 1: I
    // DEBUG_LABEL: nestedClassArray = nestedClassArray
    val nestedClassArray = arrayOf(nestedClass)

    //Breakpoint!
    val b = 1
}

class A {
    inner class B {
        fun test() = 1
    }

    class C {
        fun test() = 1
    }

    fun test() = 1
}