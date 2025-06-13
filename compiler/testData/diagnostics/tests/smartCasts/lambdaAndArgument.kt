// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun <T> foo(t1: T, t2: T) = t1 ?: t2

inline fun <T> bar(l: (T) -> Unit): T = null!!

fun use() {
    var x: Int?
    x = 5
    // Write is AFTER
    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
    // x is nullable at the second argument
    foo(bar { x = null }, x!!)
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, elvisExpression, functionDeclaration, functionalType, inline,
integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, typeParameter */
