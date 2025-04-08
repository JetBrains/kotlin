// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// WITH_STDLIB
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

open class Super {
    val superProp: String = ""
    fun superFunc() {}
}

enum class MyEnum {
    EnumValue1;
}

open class MyClass {
    object InheritorObject: MyClass() {
        val prop: String = ""
    }

    class InheritorClass: MyClass() {
        val prop: String = ""
    }

    object NestedObject

    companion object: Super() {
        val propMyClass: MyClass = TODO()
        val propInheritorClass: InheritorClass = InheritorClass()
        val propInheritorObject: InheritorObject = InheritorObject
        val prop: String = ""
        const val constProp: String = ""
        val lazyProp: String by lazy {""}
        val lambdaProp = {InheritorObject}
        fun func(): MyClass = TODO()
    }
}

val anonymousEnumConsumer = fun(arg: MyEnum){}
val anonymousConsumer = fun(arg: MyClass){}

fun testAnonimous() {
    anonymousEnumConsumer(EnumValue1)

    anonymousConsumer(InheritorObject)
    anonymousConsumer(propMyClass)

    anonymousConsumer(<!ARGUMENT_TYPE_MISMATCH!>prop<!>)
    anonymousConsumer(<!ARGUMENT_TYPE_MISMATCH!>constProp<!>)
    anonymousConsumer(<!ARGUMENT_TYPE_MISMATCH!>lazyProp<!>)
    anonymousConsumer(<!ARGUMENT_TYPE_MISMATCH!>lambdaProp<!>)
    anonymousConsumer(<!ARGUMENT_TYPE_MISMATCH!>superProp<!>)

    anonymousConsumer(<!UNRESOLVED_REFERENCE!>func<!>())
    anonymousConsumer(<!UNRESOLVED_REFERENCE!>superFunc<!>())
    anonymousConsumer(<!ARGUMENT_TYPE_MISMATCH!>lambdaProp<!>)
}