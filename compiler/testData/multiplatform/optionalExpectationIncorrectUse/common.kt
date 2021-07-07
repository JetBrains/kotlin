// WITH_RUNTIME
// ADDITIONAL_COMPILER_ARGUMENTS: -opt-in=kotlin.ExperimentalMultiplatform

@OptionalExpectation
expect annotation class A()

fun useInSignature(a: A) = a.toString()

@OptionalExpectation
expect class NotAnAnnotationClass

@OptionalExpectation
annotation class NotAnExpectedClass



annotation class InOtherAnnotation(val a: A)

@InOtherAnnotation(A())
fun useInOtherAnnotation() {}



expect class C {
    @OptionalExpectation
    annotation class Nested
}
