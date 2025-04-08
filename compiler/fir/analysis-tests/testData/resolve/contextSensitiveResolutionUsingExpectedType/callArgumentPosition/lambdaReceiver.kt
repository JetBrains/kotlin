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
fun <T>receiveLambda(l: () -> T) {}

fun testReceivedLambda() {
    receiveLambda<MyClass> { InheritorObject }
    receiveLambda<MyClass> { propMyClass }
    receiveLambda<MyClass> { propInheritorClass }
    receiveLambda<MyClass> { propInheritorObject }

    receiveLambda<MyClass> { <!ARGUMENT_TYPE_MISMATCH!>prop<!> }
    receiveLambda<MyClass> { <!ARGUMENT_TYPE_MISMATCH!>superProp<!> }
    receiveLambda<MyClass> { <!ARGUMENT_TYPE_MISMATCH!>constProp<!> }
    receiveLambda<MyClass> { <!ARGUMENT_TYPE_MISMATCH!>lambdaProp<!> }
    receiveLambda<MyClass> { <!ARGUMENT_TYPE_MISMATCH!>lazyProp<!> }
    receiveLambda<MyClass> { <!ARGUMENT_TYPE_MISMATCH!>NestedObject<!>}

    receiveLambda<MyClass> { <!UNRESOLVED_REFERENCE!>func<!>() }
    receiveLambda<MyClass> { <!UNRESOLVED_REFERENCE!>InheritorClass<!>() }
    receiveLambda<MyClass> { <!UNRESOLVED_REFERENCE!>superFunc<!>() }
}
