// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75833

annotation class Foo(val string: String)

@Foo(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"<!ILLEGAL_ESCAPE!>\d<!>"<!>)
class Bar
