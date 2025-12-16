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
    coerceMe { <!RETURN_VALUE_NOT_USED_COERCION(" of 'stringF'")!>stringF<!>() }
    coerceMe { unitF() }
    coerceMe { ign() }
    coerceMe { thrower() }
}

fun testRefs() {
    coerceMe(::<!RETURN_VALUE_NOT_USED_COERCION(" of 'stringF'")!>stringF<!>)
    coerceMe(::unitF)
    coerceMe(::ign)
    coerceMe(::thrower)
}

fun testVals() {
    val rf = ::stringF
    val rf2: () -> Unit = ::<!RETURN_VALUE_NOT_USED_COERCION!>stringF<!>
    val rf3: () -> Unit = { <!RETURN_VALUE_NOT_USED_COERCION!>stringF<!>() }
}

class A {
    @IgnorableReturnValue fun ign() = ""
}

fun testReceivers() {
    val a = A()
    coerceMe(a::<!RETURN_VALUE_NOT_USED_COERCION!>toString<!>)
    val rf: (A) -> Unit = A::<!RETURN_VALUE_NOT_USED_COERCION!>toString<!>
    coerceMe(a::ign)
    val rf2: (A) -> Unit = A::ign
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, callableReference, functionDeclaration, functionalType,
lambdaLiteral, stringLiteral */
