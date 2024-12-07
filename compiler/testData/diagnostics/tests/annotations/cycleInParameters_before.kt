// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_REFLECT
// LANGUAGE: -ProhibitCyclesInAnnotations
// ISSUE: KT-47932

import kotlin.reflect.KClass

annotation class X(<!CYCLE_IN_ANNOTATION_PARAMETER_WARNING!>val value: X<!>) // error
annotation class Y(val value: Array<Y>) // no error

annotation class Z1(<!CYCLE_IN_ANNOTATION_PARAMETER_WARNING!>val a: Z2<!>, <!CYCLE_IN_ANNOTATION_PARAMETER_WARNING!>val b: Z2<!>) // error
annotation class Z2(<!CYCLE_IN_ANNOTATION_PARAMETER_WARNING!>val value: Z1<!>) // error

annotation class A(val x: KClass<A>) // OK
annotation class B(val x: KClass<B>) // OK
annotation class C(val b: B) // OK
