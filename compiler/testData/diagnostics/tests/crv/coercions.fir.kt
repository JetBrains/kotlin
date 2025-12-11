// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValues

fun stringF(): String = ""
fun unitF(): Unit = Unit

@IgnorableReturnValue
fun ign(): String = ""
fun thrower(): Nothing = error("")

fun coerceMe(block: () -> Unit) {
    block()
}

fun test() {
    coerceMe { <!RETURN_VALUE_NOT_USED!>stringF<!>() }
    coerceMe { unitF() }
    coerceMe { ign() }
    coerceMe { thrower() }
}

fun testRefs() {
    coerceMe(::<!RETURN_VALUE_NOT_USED!>stringF<!>)
    coerceMe(::unitF)
    coerceMe(::ign)
    coerceMe(::thrower)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, callableReference, functionDeclaration, functionalType,
lambdaLiteral, stringLiteral */
