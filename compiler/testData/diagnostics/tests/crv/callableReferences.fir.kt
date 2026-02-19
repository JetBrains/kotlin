// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValues

fun testFun(): Int = 1
@IgnorableReturnValue fun ignorable(): Int = 2

fun test() {
    <!RETURN_VALUE_NOT_USED!>testFun<!>()
    <!RETURN_VALUE_NOT_USED!>(::testFun)<!>()
    val ref = ::testFun
    <!RETURN_VALUE_NOT_USED!>ref<!>()
}

fun testIgnorable() {
    ignorable()
    <!RETURN_VALUE_NOT_USED!>(::ignorable)<!>() // as designed
    val ref = ::ignorable
    <!RETURN_VALUE_NOT_USED!>ref<!>() // as designed
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, callableReference, functionDeclaration, integerLiteral,
localProperty, propertyDeclaration */
