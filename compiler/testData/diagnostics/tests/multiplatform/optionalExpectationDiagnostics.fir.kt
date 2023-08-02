// WITH_STDLIB
// !OPT_IN: kotlin.ExperimentalMultiplatform

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

@OptionalExpectation
expect annotation class A()

fun useInSignature(a: <!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY!>A<!>) = a.toString()

<!NO_ACTUAL_FOR_EXPECT{JVM}!><!WRONG_ANNOTATION_TARGET!>@OptionalExpectation<!>
expect class NotAnAnnotationClass<!>

@OptionalExpectation
annotation class NotAnExpectedClass

annotation class InOtherAnnotation(val a: <!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY!>A<!>)

@InOtherAnnotation(<!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY!>A()<!>)
fun useInOtherAnnotation() {}

expect class C {
    @OptionalExpectation
    annotation class Nested
}

// MODULE: platform()()(common)
// FILE: platform.kt

fun useInReturnType(): <!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY!>A?<!> = null

annotation class AnotherAnnotation(val a: <!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY!>A<!>)

@AnotherAnnotation(<!OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY!>A()<!>)
fun useInAnotherAnnotation() {}

actual class C {
    actual annotation class Nested
}
