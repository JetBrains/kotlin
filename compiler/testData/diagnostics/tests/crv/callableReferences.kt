// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValues

fun testFun(): Int = 1
@IgnorableReturnValue fun ignorable(): Int = 2

fun test() {
    testFun()
    (::testFun)()
    val ref = ::testFun
    ref()
}

fun testIgnorable() {
    ignorable()
    (::ignorable)() // as designed
    val ref = ::ignorable
    ref() // as designed
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, callableReference, functionDeclaration, integerLiteral,
localProperty, propertyDeclaration */
