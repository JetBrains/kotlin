// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73256, KT-74199
// LANGUAGE: +AnnotationAllUseSiteTarget -AnnotationDefaultTargetMigrationWarning
// FIR_DUMP

annotation class A1

annotation class A2

class My(@all:[<!INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION!>A1<!> <!INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION!>A2<!>] val x: Int, @property:[A1 A2] val y: Int)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class A3

@Target(AnnotationTarget.PROPERTY)
annotation class A4

class Your(@[A1 A2] val x: Int, @[A3 A4] val y: Int)
