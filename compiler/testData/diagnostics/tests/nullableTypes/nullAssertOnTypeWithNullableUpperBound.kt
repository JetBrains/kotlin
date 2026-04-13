// RUN_PIPELINE_TILL: FIR2IR
fun <T> test(t: T): T {
    if (t != null) {
        return t<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }
    return t!!
}

fun <T> T.testThis(): String {
    if (this != null) {
        return this<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.toString()
    }
    return this!!.toString()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, dnnType, equalityExpression, funWithExtensionReceiver, functionDeclaration,
ifExpression, intersectionType, nullableType, smartcast, thisExpression, typeParameter */
