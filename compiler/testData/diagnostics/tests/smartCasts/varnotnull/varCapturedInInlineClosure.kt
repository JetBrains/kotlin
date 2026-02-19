// RUN_PIPELINE_TILL: BACKEND
// See also KT-7186 and forEachSafe.kt
// Custom `forEach` has no contract but the lambda is inline (not crossinline) so smart cast is safe

inline fun IntArray.forEachIndexed( op: (i: Int, value: Int) -> Unit) {
    for (i in 0..this.size)
        op(i, this[i])
}

fun max(a: IntArray): Int? {
    var maxI: Int? = null
    a.forEachIndexed { i, value ->
        if (maxI == null || value >= a[<!SMARTCAST_IMPOSSIBLE!>maxI<!>])
            maxI = i
    }
    return maxI
}

/* GENERATED_FIR_TAGS: assignment, comparisonExpression, disjunctionExpression, equalityExpression, forLoop,
funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression, inline, integerLiteral, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, rangeExpression, smartcast, thisExpression */
