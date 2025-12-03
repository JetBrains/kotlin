// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81763
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FIR_DUMP
// LANGUAGE: -DontUseConstraintFromEqualityOperatorInElvis

fun fooElvis(x: Long, y: Long?): Boolean =
    // K of elvis should be inferred to Long, not to Any?
    x != (y ?: 0)

fun <I> id(i: I): I = i

fun fooElvisId(x: Long, y: Long?): Boolean =
    x != id(y ?: 0)

fun fooBar(x: Long, y: Long?): Boolean =
    x != bar(y, 0)

fun <T> bar(x: T?, y: T): T = x ?: y

fun fooBarExact(x: Long, y: Long?): Boolean =
    x != barExact(y, 0)

@Suppress("INVISIBLE_REFERENCE")
fun <E> barExact(x: E?, y: E): @kotlin.internal.Exact E = x ?: y

fun assign(x: Long?) {
    val n: Number = x ?: 0
    val a: Any? = x ?: 0
    val l: Long? = x ?: 0
}

fun call(x: Long?) {
    acceptNumber(x ?: 0)
    acceptAny(x ?: 0)
    acceptLong(x ?: 0)
}

fun acceptNumber(n: Number) {}

fun acceptAny(a: Any?) {}

fun acceptLong(l: Long?) {}

/* GENERATED_FIR_TAGS: elvisExpression, equalityExpression, functionDeclaration, integerLiteral, nullableType,
typeConstraint, typeParameter */
