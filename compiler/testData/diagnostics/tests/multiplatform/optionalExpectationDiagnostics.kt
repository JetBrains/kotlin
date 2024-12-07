// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalMultiplatform
// MUTE_LL_FIR: LL tests don't run IR actualizer to report NO_ACTUAL_FOR_EXPECT

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

@<!UNRESOLVED_REFERENCE!>OptionalExpectation<!>
expect annotation class A()

fun useInSignature(a: <!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY{JVM}!>A<!>) = a.toString()

<!WRONG_ANNOTATION_TARGET{JVM}!>@<!UNRESOLVED_REFERENCE!>OptionalExpectation<!><!>
expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>NotAnAnnotationClass<!>

<!OPTIONAL_EXPECTATION_NOT_ON_EXPECTED{JVM}!>@<!UNRESOLVED_REFERENCE!>OptionalExpectation<!><!>
annotation class NotAnExpectedClass

annotation class InOtherAnnotation(val a: <!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY{JVM}!>A<!>)

@InOtherAnnotation(<!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY{JVM}!>A<!>())
fun useInOtherAnnotation() {}

expect class C {
    @<!UNRESOLVED_REFERENCE!>OptionalExpectation<!>
    annotation class Nested
}

// MODULE: platform()()(common)
// FILE: platform.kt

@<!OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE!>A<!>
class D

fun useInReturnType(): <!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY, OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE!>A<!>? = null

annotation class AnotherAnnotation(val a: <!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY, OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE!>A<!>)

@AnotherAnnotation(<!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY, OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE!>A<!>())
fun useInAnotherAnnotation() {}

actual class C {
    actual annotation class Nested
}
