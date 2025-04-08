// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// WITH_STDLIB
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

open class MySuper {
    val superProp: String = ""
}

enum class MyEnum {
    EnumValue1, EnumValue2;

    companion object {
        val EnumValue3 = EnumValue1
    }
}

open class MyClass {
    object InheritorObject: MyClass() {}

    class InheritorClass: MyClass() {}

    object ObjectInClass

    object NestedObject

    companion object: MySuper() {
        val propClass: MyClass = TODO()
        val propInheritorClass: InheritorClass = InheritorClass()
        val propInheritorObject: InheritorObject = InheritorObject
        val prop: String = ""
        fun func(): MyClass = TODO()
        val lazyProp: String by lazy {""}
        val lambdaProp = {InheritorObject}
    }
}

fun namedArgumentsHolder(enumArg: MyEnum, arg: MyClass) {}
fun namedArgumentHolder(arg: MyClass) {}

fun testNamedArgs() {
    namedArgumentsHolder(
        arg = InheritorObject,
        enumArg = EnumValue1
    )
    namedArgumentsHolder(
        enumArg = EnumValue3,
        arg = propClass
    )
    namedArgumentHolder(arg = <!ARGUMENT_TYPE_MISMATCH!>prop<!>)
    namedArgumentHolder(arg = <!ARGUMENT_TYPE_MISMATCH!>superProp<!>)
    namedArgumentHolder(arg = <!UNRESOLVED_REFERENCE!>func<!>())
    namedArgumentHolder(arg = <!UNRESOLVED_REFERENCE!>superFunc<!>())
    namedArgumentHolder(arg = <!ARGUMENT_TYPE_MISMATCH!>lazyProp<!>)
}