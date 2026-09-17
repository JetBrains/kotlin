// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
error class Foo

fun <E : RichError> foo(arg: String | E) {}
fun <T : NonError?, E : RichError> bar(arg: T | E) {}
fun <T : Any?> baz(arg: T) {}

fun <E1 : RichError, E2 : RichError> fooIncorrect(
    arg: <!MULTIPLE_TYPE_PARAMETERS_CAN_HOLD_ERROR!>String | E1 | E2<!>,
    arg2: <!MULTIPLE_TYPE_PARAMETERS_CAN_HOLD_ERROR!>String | <!MULTIPLE_TYPE_PARAMETERS_CAN_HOLD_ERROR!>(E1 | E2)<!><!>
) {}

fun <T, E : RichError> fooIncorrect(
    arg: <!MULTIPLE_TYPE_PARAMETERS_CAN_HOLD_ERROR!>T | E<!>,
    arg2: <!MULTIPLE_TYPE_PARAMETERS_CAN_HOLD_ERROR!>T | (Foo | E)<!>
) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeConstraint, typeParameter */
