// LANGUAGE: +ImprovedExhaustivenessChecksIn21
// ISSUE: KT-70672, KT-70673
fun testNullableBoolean(arg: Boolean?) = when (arg) {
    null -> 1
    <!USELESS_IS_CHECK!>is Boolean<!> -> 2
}

fun testNullableBoolean2(arg: Boolean?) = when (arg) {
    <!USELESS_IS_CHECK!>is Boolean?<!> -> 2
}

fun testNullableBooleanAgainstAny(arg: Boolean?) = when (arg) {
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

fun testNullableSealed2(arg: Sealed?) = when (arg) {
    <!USELESS_IS_CHECK!>is Sealed?<!> -> 2
}

enum class MyEnum {
    A, B
}

fun testNullableEnum(arg: MyEnum?) = when (arg) {
    is MyEnum -> 1
    null -> null
}

fun testNullableEnum2(arg: MyEnum?) = when (arg) {
    <!USELESS_IS_CHECK!>is MyEnum?<!> -> 1
}

fun testNullableAny(arg: Any?) = when (arg) {
    null -> 1
    <!USELESS_IS_CHECK!>is Any<!> -> 2
}

fun testNullableAny2(arg: Any?) = when (arg) {
    <!USELESS_IS_CHECK!>is Any?<!> -> 2
}