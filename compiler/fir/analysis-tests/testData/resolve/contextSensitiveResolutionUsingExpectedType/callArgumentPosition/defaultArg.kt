// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    EnumValue1;
}

open class MyClass {
    object InheritorObject: MyClass()

    companion object {
        fun func(): MyClass = TODO()
    }
}

fun defaultArgHolder1(
    enumArg: MyEnum = EnumValue1,
    sealedArg: MyClass = InheritorObject,
    sealedArg2: MyClass = <!UNRESOLVED_REFERENCE!>func<!>()
) {}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration, nestedClass,
objectDeclaration */
