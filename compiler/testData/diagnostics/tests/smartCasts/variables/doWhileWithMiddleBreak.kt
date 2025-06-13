// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun x(): Boolean { return true }

public fun foo(pp: Any): Int {
    var p = pp
    do {
        (p as String).length
        if (p == "abc") break
        p = 42
    } while (!x())
    // Smart cast is NOT possible here
    return p.<!UNRESOLVED_REFERENCE!>length<!>()
}

/* GENERATED_FIR_TAGS: asExpression, assignment, break, doWhileLoop, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, intersectionType, localProperty, propertyDeclaration, smartcast, stringLiteral */
