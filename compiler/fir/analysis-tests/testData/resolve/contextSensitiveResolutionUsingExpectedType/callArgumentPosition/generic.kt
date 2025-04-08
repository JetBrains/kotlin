// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// WITH_STDLIB
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

open class MySuper {
    val superProp: String = ""
}

open class MyClass {
    object InheritorObject: MyClass() {}

    class InheritorClass: MyClass() {}

    object NestedObject

    companion object: MySuper() {
        val propMyClass: MyClass = TODO()
        val propInheritorClass: InheritorClass = InheritorClass()
        val propInheritorObject: InheritorObject = InheritorObject
        val prop: String = ""
        fun func(): MyClass = TODO()
        val lazyProp: String by lazy {""}
        val lambdaProp = {InheritorObject}
    }
}

fun consumeMyClass(arg: MyClass) {}

fun <T> test(arg: T): T = arg

fun testGeneric() {
    consumeMyClass(test(InheritorObject))

    consumeMyClass(test(propMyClass))
    consumeMyClass(test(propInheritorClass))
    consumeMyClass(test(propInheritorObject))

    consumeMyClass(test(<!ARGUMENT_TYPE_MISMATCH!>NestedObject<!>))
    consumeMyClass(test(<!ARGUMENT_TYPE_MISMATCH!>prop<!>))
    consumeMyClass(test(<!ARGUMENT_TYPE_MISMATCH!>superProp<!>))
    consumeMyClass(test(<!ARGUMENT_TYPE_MISMATCH!>lazyProp<!>))
    consumeMyClass(test(<!ARGUMENT_TYPE_MISMATCH!>lambdaProp<!>))

    consumeMyClass(test(<!UNRESOLVED_REFERENCE!>InheritorClass<!>()))
    consumeMyClass(test(<!UNRESOLVED_REFERENCE!>func<!>()))
}