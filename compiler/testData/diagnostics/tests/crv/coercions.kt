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
    coerceMe { stringF() }
    coerceMe { unitF() }
    coerceMe { ign() }
    coerceMe { thrower() }
}

fun testRefs() {
    coerceMe(::stringF)
    coerceMe(::unitF)
    coerceMe(::ign)
    coerceMe(::thrower)
}

fun testVals() {
    val rf = ::stringF
    val rf2: () -> Unit = <!TYPE_MISMATCH!>::<!TYPE_MISMATCH!>stringF<!><!>
    val rf3: () -> Unit = { stringF() }
}

class A {
    @IgnorableReturnValue fun ign() = ""
}

fun testReceivers() {
    val a = A()
    coerceMe(a::toString)
    val rf: (A) -> Unit = <!TYPE_MISMATCH!>A::<!TYPE_MISMATCH!>toString<!><!>
    coerceMe(a::ign)
    val rf2: (A) -> Unit = <!TYPE_MISMATCH!>A::<!TYPE_MISMATCH!>ign<!><!>
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, callableReference, functionDeclaration, functionalType,
lambdaLiteral, stringLiteral */
