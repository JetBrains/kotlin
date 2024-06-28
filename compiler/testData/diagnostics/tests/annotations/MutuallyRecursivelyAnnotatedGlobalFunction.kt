// FIR_IDENTICAL
// Functions can be recursively annotated
annotation class ann(val x: Int)
@ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>bar()<!>) fun foo() = 1
@ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo()<!>) fun bar() = 2
