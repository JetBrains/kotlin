// Functions can be recursively annotated
annotation class ann(val x: Int)
@ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar()<!>) fun foo() = 1
@ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo()<!>) fun bar() = 2
