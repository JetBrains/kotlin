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

infix fun Int.infixEnumConsumer(arg: MyEnum){}
infix fun Int.infixConsumer(arg: MyClass){}

fun testInfix() {
    val r1 = 1 infixEnumConsumer EnumValue1
    val r2 = 1 infixEnumConsumer <!UNRESOLVED_REFERENCE!>InheritorObject<!>
    val r3 = 1 infixConsumer InheritorObject
}