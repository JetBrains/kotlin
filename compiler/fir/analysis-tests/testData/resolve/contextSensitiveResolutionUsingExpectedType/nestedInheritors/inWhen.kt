// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-77823
// RENDER_DIAGNOSTICS_FULL_TEXT

sealed class MyClass {
    object NestedInheritor : MyClass()
}

fun receiveMyClass(m: MyClass) {}

fun sealed1(b: Boolean, myClass: MyClass) {
    receiveMyClass(
        when {
            b -> myClass
            else -> NestedInheritor
        }
    )
}

fun sealed2(b: Boolean, subClass: MyClass.NestedInheritor) {
    receiveMyClass(
        when {
            b -> subClass
            else -> NestedInheritor
        }
    )
}

fun sealed3(b1: Boolean, b2: Boolean, subClass: MyClass.NestedInheritor) {
    receiveMyClass(
        when {
            b1 -> subClass
            b2 -> NestedInheritor
            else -> <!UNRESOLVED_REFERENCE!>OtherInheritor<!>
        }
    )
}

sealed class OtherClass {
    abstract class Base : OtherClass()

    class A : Base()
    class B : Base()

    object C : OtherClass()
}

fun receiveOtherClass(m: OtherClass) {}

fun other(b1: Boolean, b2: Boolean, subClass1: OtherClass.A, subClass2: OtherClass.B) {
    receiveOtherClass(
        when {
            b1 -> subClass1
            b2 -> subClass2
            else -> <!ARGUMENT_TYPE_MISMATCH!>C<!>
        }
    )
}

open class OpenClass {
    object NestedInheritorOfOpenClass : OpenClass()
}

fun receiveOpenClass(m: OpenClass) {}

fun open1(b: Boolean, openClass: OpenClass) {
    receiveOpenClass(
        when {
            b -> openClass
            else -> NestedInheritorOfOpenClass
        }
    )
}

fun open2(b: Boolean, subClass: OpenClass.NestedInheritorOfOpenClass) {
    receiveOpenClass(
        when {
            b -> subClass
            else -> <!UNRESOLVED_REFERENCE!>NestedInheritorOfOpenClass<!>
        }
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, objectDeclaration, sealed, whenExpression */
