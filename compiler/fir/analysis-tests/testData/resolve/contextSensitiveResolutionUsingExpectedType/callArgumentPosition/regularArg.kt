// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// WITH_STDLIB
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

open class Super {
    val superProp: String = ""
    fun superFunc() {}
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
        fun func(): MyClass = TODO()
        val lambdaProp = {InheritorObject}
    }
}

fun receiveMyClass(arg: MyClass) {}

fun test0() {
    receiveMyClass(InheritorObject)
    receiveMyClass(propMyClass)
    receiveMyClass(propInheritorClass)
    receiveMyClass(propInheritorObject)

    receiveMyClass(<!ARGUMENT_TYPE_MISMATCH!>NestedObject<!>)
    receiveMyClass(<!ARGUMENT_TYPE_MISMATCH!>prop<!>)
    receiveMyClass(<!ARGUMENT_TYPE_MISMATCH!>superProp<!>)
    receiveMyClass(<!ARGUMENT_TYPE_MISMATCH!>constProp<!>)
    receiveMyClass(<!ARGUMENT_TYPE_MISMATCH!>lazyProp<!>)
    receiveMyClass(<!ARGUMENT_TYPE_MISMATCH!>lambdaProp<!>)
    receiveMyClass(<!ARGUMENT_TYPE_MISMATCH!>superProp<!>)

    receiveMyClass(<!UNRESOLVED_REFERENCE!>InheritorClass<!>())
    receiveMyClass(<!UNRESOLVED_REFERENCE!>func<!>())
    receiveMyClass(<!UNRESOLVED_REFERENCE!>superFunc<!>())
}
