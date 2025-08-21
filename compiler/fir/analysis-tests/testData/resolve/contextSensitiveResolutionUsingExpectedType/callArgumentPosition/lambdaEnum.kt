// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    EnumValue1, EnumValue2;

    companion object {
        val EnumValue3 = EnumValue1
        val prop: String = ""
    }
}

fun testLambda(lEnum: (arg: MyEnum) -> Unit) {
    lEnum(EnumValue1)
    lEnum(EnumValue2)
}

fun <T>receiveLambda(l: () -> T) {}

fun testReceivedLambda() {
    receiveLambda<MyEnum> { EnumValue1 }
    receiveLambda<MyEnum> { EnumValue3 }
    receiveLambda<MyEnum> { <!ARGUMENT_TYPE_MISMATCH!>prop<!> }
}

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, enumEntry, functionDeclaration, functionalType, lambdaLiteral,
nullableType, objectDeclaration, propertyDeclaration, stringLiteral, typeParameter */
