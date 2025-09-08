// RUN_PIPELINE_TILL: FRONTEND
class Your

fun Your.foo() = Any()

fun <T> T?.let(f: (T) -> Unit) {
    if (this != null) f(<!DEBUG_INFO_SMARTCAST!>this<!>)
}

fun test(your: Your?) {
    (your?.foo() <!USELESS_CAST!>as? Any<!>)?.let {}
    // strange smart cast to 'Your' at this point
    your<!UNSAFE_CALL!>.<!>hashCode()
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, equalityExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, lambdaLiteral, nullableType, safeCall, smartcast, thisExpression, typeParameter */
