// RUN_PIPELINE_TILL: FIR2IR
fun <T> test(t: T): String? {
    if (t != null) {
        return t<!UNNECESSARY_SAFE_CALL!>?.<!>toString()
    }
    return <!DEBUG_INFO_CONSTANT!>t<!>?.toString()
}

fun <T> T.testThis(): String? {
    if (this != null) {
        return this<!UNNECESSARY_SAFE_CALL!>?.<!>toString()
    }
    return this?.toString()
}

/* GENERATED_FIR_TAGS: dnnType, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
intersectionType, nullableType, safeCall, smartcast, thisExpression, typeParameter */
