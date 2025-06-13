// RUN_PIPELINE_TILL: FRONTEND
// KT-9051: Allow smart cast for captured variables if they are not modified

fun foo(y: String?) {
    var x: String? = ""
    if (x != null) {
        y?.let { x = null }
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length // Smart cast is not possible
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, safeCall, stringLiteral */
