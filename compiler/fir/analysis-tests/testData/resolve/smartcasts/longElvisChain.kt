// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-49249, KT-51634
// WITH_STDLIB

fun test_1() {
    val a: Throwable? = null;
    val b: Unit? = null
    val c = a ?: b?.let { return it } ?: return
    c<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    throw a
}

fun test_2() {
    val a: Throwable? = null;
    val b: Unit? = null
    val c = a ?: b?.let { return it } ?: return
    throw a
}

fun test_3() {
    val a: Throwable? = null;
    val b: Unit? = null
    val c = b?.let { return it } ?: a ?: return
    c<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    throw a
}

fun test_4() {
    val a: Throwable? = null;
    val b: Unit? = null
    val c = b?.let { return it } ?: a ?: return
    throw a
}

fun test_5() {
    var a: Throwable? = null
    a = a ?: run { throw Exception() }
    throw a
}

/* GENERATED_FIR_TAGS: checkNotNullCall, elvisExpression, functionDeclaration, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, safeCall, smartcast */
