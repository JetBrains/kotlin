// RUN_PIPELINE_TILL: BACKEND
public fun foo(p: String?, y: String?): Int {
    do {
        // After the check, smart cast should work
        if (y == null) {
            "null"
            break
        }
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
        p!!.length
    } while (true)
    return y?.length ?: -1
}

/* GENERATED_FIR_TAGS: break, checkNotNullCall, doWhileLoop, elvisExpression, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, nullableType, safeCall, smartcast, stringLiteral */
