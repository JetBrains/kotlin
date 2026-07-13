// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE_FEATURE_TOGGLED: ProhibitIllegalNotNullSmartCastsInEqualities

open class Regular {
    override fun equals(other: Any?): Boolean = other is Regular
}

fun Any.callOnNonNull() = Unit

fun <T : Regular?> f1(a: T, x: T & Any) {
    if (a == x) a.callOnNonNull()
    if (x === a) a.callOnNonNull()
}

fun <T> f2(a: T, x: T & Any) {
    if (a == x) a.callOnNonNull()
    if (x === a) a.callOnNonNull()
}

fun <T> f3(a: T, x: Any) {
    if (a == x) a.callOnNonNull()
    if (x === a) a.callOnNonNull()
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, equalityExpression, funWithExtensionReceiver, functionDeclaration,
ifExpression, isExpression, nullableType, operator, override, smartcast, typeConstraint, typeParameter */
