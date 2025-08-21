// RUN_PIPELINE_TILL: BACKEND
fun x(): Boolean { return true }

public fun foo(pp: String?, rr: String?): Int {
    var p = pp
    var r = rr
    do {
        do {
            p!!.length
        } while (r == null)  
    } while (!x())
    // Auto cast possible
    r.length
    // Auto cast possible
    return p.length
}

/* GENERATED_FIR_TAGS: checkNotNullCall, doWhileLoop, equalityExpression, functionDeclaration, localProperty,
nullableType, propertyDeclaration, smartcast */
