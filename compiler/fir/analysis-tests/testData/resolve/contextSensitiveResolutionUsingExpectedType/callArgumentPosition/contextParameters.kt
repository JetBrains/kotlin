// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType, +ContextParameters

enum class MyEnum {
    EnumValue1;
}

sealed class MySealed {
    object InheritorObject: MySealed() {}
}

context(i: MyEnum)
fun foo() {}

context(i: MySealed)
fun foo() {}

fun testWithEnum() {
    with<MyEnum, Unit>(EnumValue1){
        foo()
    }

    with<MySealed, Unit>(InheritorObject){
        foo()
    }
}

context(i: MyEnum)
val String.a: String
    get() = ""

fun testWithEnumWithProp() {
    with<MyEnum, Unit>(EnumValue1) {
        "".a
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, functionDeclarationWithContext,
getter, lambdaLiteral, nestedClass, objectDeclaration, propertyDeclaration, propertyDeclarationWithContext,
propertyWithExtensionReceiver, sealed, stringLiteral */
