// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73256
// LANGUAGE: +AnnotationAllUseSiteTarget
// FIR_DUMP

annotation class A1

annotation class A2

class My(@all:[<!INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION!>A1<!> <!INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION!>A2<!>] val x: Int, @property:[A1 A2] val y: Int)
