// RUN_PIPELINE_TILL: FRONTEND
// WITH_PLATFORM_LIBS
// KT-86474

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import platform.Foundation.NSAssertionHandler
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.posix.printf

fun cArguments(detail: String, nullableDetail: String?) {
    NSLog("%@", <!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>"literal"<!>)
    NSLog("%@", <!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>)
    NSLog("%@", <!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>nullableDetail<!>)
    NSLog("%@", *arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>))
    NSLog("%@", *arrayOf(*arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>)))

    printf("%s", <!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>)
    printf("%s", *arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>))
}

fun objCArguments(handler: NSAssertionHandler, detail: String) {
    handler.handleFailureInFunction("zzz", "zzz", 0, null)
    handler.handleFailureInFunction("zzz", "zzz", 0, null, <!STRING_AS_VARIADIC_OBJC_PARAM_IS_AMBIGUOUS!>"literal"<!>)
    handler.handleFailureInFunction("zzz", "zzz", 0, null, <!STRING_AS_VARIADIC_OBJC_PARAM_IS_AMBIGUOUS!>detail<!>)
    handler.handleFailureInFunction("zzz", "zzz", 0, null, *arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>))
    handler.handleFailureInFunction("zzz", "zzz", 0, null, *arrayOf(*arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>)))
}

fun nullableObjCArguments(handler: NSAssertionHandler, detail: String?) {
    handler.handleFailureInFunction("zzz", "zzz", 0, null, <!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>)
    handler.handleFailureInFunction("zzz", "zzz", 0, null, *arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>))
    handler.handleFailureInFunction("zzz", "zzz", 0, null, *arrayOf(*arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>)))
}

typealias MyString = String
typealias MyNullableString = String?

fun typeAliasedArguments(handler: NSAssertionHandler, detail: MyString, nullableDetail: MyNullableString) {
    NSLog("%@", <!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>)
    NSLog("%@", <!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>nullableDetail<!>)
    NSLog("%@", *arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>))
    printf("%s", <!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>)

    handler.handleFailureInFunction("zzz", "zzz", 0, null, <!STRING_AS_VARIADIC_OBJC_PARAM_IS_AMBIGUOUS!>detail<!>)
    handler.handleFailureInFunction("zzz", "zzz", 0, null, <!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>nullableDetail<!>)
    handler.handleFailureInFunction("zzz", "zzz", 0, null, *arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>))
    handler.handleFailureInFunction("zzz", "zzz", 0, null, *arrayOf(*arrayOf(<!STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT!>detail<!>)))

    val myString: MyString = "abc"
    handler.handleFailureInFunction("zzz", "zzz", 0, null, <!STRING_AS_VARIADIC_OBJC_PARAM_IS_AMBIGUOUS!>myString<!>)
}

fun explicitConversions(detail: String, nsString: NSString) {
    NSLog("%@", nsString)
    NSLog("%@", detail <!CAST_NEVER_SUCCEEDS!>as<!> NSString)
    NSLog("%s", detail.cstr)
    printf("%s", detail.cstr)
}

fun kotlinVararg(format: String, vararg args: Any?) {}

fun kotlinCall(detail: String) {
    kotlinVararg("%@", detail)
}
