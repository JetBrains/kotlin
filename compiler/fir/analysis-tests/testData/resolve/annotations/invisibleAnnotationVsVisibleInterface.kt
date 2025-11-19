// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80572
// FIR_DUMP

// MODULE: a
interface TargetTypeAA

interface TargetTypeBA

object OuterTypeA {
    internal annotation class TargetTypeAA
}

internal object OuterTypeB {
    annotation class TargetTypeBA
}

// MODULE: b(a)
import OuterTypeA.TargetTypeAA
import <!INVISIBLE_REFERENCE!>OuterTypeB<!>.TargetTypeBA

interface FooAnnotationUser {
    @<!INVISIBLE_REFERENCE!>OuterTypeA<!>.<!INVISIBLE_REFERENCE!>TargetTypeAA<!> fun annotationUserAAOuter()

    @<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>TargetTypeAA<!> fun annotationUserAAInner()

    @<!INVISIBLE_REFERENCE!>OuterTypeB<!>.<!INVISIBLE_REFERENCE!>TargetTypeBA<!> fun annotationUserBAOuter()

    @<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>TargetTypeBA<!> fun annotationUserBAInner()
}

fun test() {
    @<!INVISIBLE_REFERENCE!>OuterTypeA<!>.<!INVISIBLE_REFERENCE!>TargetTypeAA<!>
    val a = 1
    @<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>TargetTypeAA<!>
    val b = 1
    @<!INVISIBLE_REFERENCE!>OuterTypeB<!>.<!INVISIBLE_REFERENCE!>TargetTypeBA<!>
    val c = 1
    @<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>TargetTypeBA<!>
    val d = 1
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, interfaceDeclaration, nestedClass, objectDeclaration */
