// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ImprovedExhaustivenessChecksIn21
// ISSUE: KT-70672, KT-70673
fun testNullableBoolean(arg: Boolean?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> 1
    <!USELESS_IS_CHECK!>is Boolean<!> -> 2
}

fun testNullableBoolean2(arg: Boolean?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    <!USELESS_IS_CHECK!>is Boolean?<!> -> 2
}

fun testNullableBooleanAgainstAny(arg: Boolean?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> 1
    <!USELESS_IS_CHECK!>is Any<!> -> 2
}

sealed class Sealed {
    object A : Sealed()
    object B : Sealed()
}

fun testNullableSealed(arg: Sealed?) = when (arg) {
    null -> 1
    <!USELESS_IS_CHECK!>is Sealed<!> -> 2
}

fun testNullableSealed2(arg: Sealed?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    <!USELESS_IS_CHECK!>is Sealed?<!> -> 2
}

enum class MyEnum {
    A, B
}

fun testNullableEnum(arg: MyEnum?) = when (arg) {
    is MyEnum -> 1
    null -> null
}

fun testNullableEnum2(arg: MyEnum?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    <!USELESS_IS_CHECK!>is MyEnum?<!> -> 1
}

fun testNullableAny(arg: Any?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> 1
    <!USELESS_IS_CHECK!>is Any<!> -> 2
}

fun testNullableAny2(arg: Any?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    <!USELESS_IS_CHECK!>is Any?<!> -> 2
}

fun <T> testNullableTypeParameter(arg: T?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is <!CANNOT_CHECK_FOR_ERASED!>T<!><!> -> false
}
