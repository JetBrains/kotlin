// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    EnumValue1;
}

open class MyClass {
    object InheritorObject: MyClass() {
    }
}

class OperatorHolder() {
    operator fun plus(enumArg: MyEnum) {}
    operator fun minus(arg: MyClass) {}
}

fun testOperator(i: OperatorHolder) {
    i + EnumValue1
    i + <!UNRESOLVED_REFERENCE!>InheritorObject<!>
    i - InheritorObject
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, enumDeclaration, enumEntry, functionDeclaration,
nestedClass, objectDeclaration, operator, primaryConstructor */
