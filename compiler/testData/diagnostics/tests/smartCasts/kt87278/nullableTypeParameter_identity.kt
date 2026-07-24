// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ProhibitIllegalNotNullSmartCastsInEqualities

open class Regular {
    override fun equals(other: Any?): Boolean = other is Regular
}

fun Any.callOnNonNull() = Unit

fun <T : Regular?> f1(t: T, x: T) {
    if (t === x) x<!UNSAFE_CALL!>.<!>callOnNonNull()
    if (x === t) x<!UNSAFE_CALL!>.<!>callOnNonNull()
}

fun <T : Regular?> f2(t: T, x: T?) {
    if (t === x) x<!UNSAFE_CALL!>.<!>callOnNonNull()
    if (x === t) x<!UNSAFE_CALL!>.<!>callOnNonNull()
}

fun <T> f3(t: T, x: T) {
    if (t === x) x<!UNSAFE_CALL!>.<!>callOnNonNull()
    if (x === t) x<!UNSAFE_CALL!>.<!>callOnNonNull()
}

fun <T> f4(t: T, x: T?) {
    if (t === x) x<!UNSAFE_CALL!>.<!>callOnNonNull()
    if (x === t) x<!UNSAFE_CALL!>.<!>callOnNonNull()
}

fun <T> f5(t: T, x: Any?) {
    if (t === x) x<!UNSAFE_CALL!>.<!>callOnNonNull()
    if (x === t) x<!UNSAFE_CALL!>.<!>callOnNonNull()
}

fun <T : S, S> f6(t: T, x: Any?) {
    if (t === x) x<!UNSAFE_CALL!>.<!>callOnNonNull()
    if (x === t) x<!UNSAFE_CALL!>.<!>callOnNonNull()
}

fun <T : S, S : Any> f7(t: T, x: Any?) {
    if (t === x) x.callOnNonNull()
    if (x === t) x.callOnNonNull()
}

fun <T : Regular> f8(t: T, x: Any?) {
    if (t === x) x.callOnNonNull()
    if (x === t) x.callOnNonNull()
}

fun <T : Regular> f9(t: T, x: T?) {
    if (t === x) x.callOnNonNull()
    if (x === t) x.callOnNonNull()
}

fun <T : S, S : Regular> f10(t: T, x: S?) {
    if (t === x) x.callOnNonNull()
    if (x === t) x.callOnNonNull()
}

fun <T : S, S : Regular?> f11(t: T, x: S?) {
    if (t === x) x<!UNSAFE_CALL!>.<!>callOnNonNull()
    if (x === t) x<!UNSAFE_CALL!>.<!>callOnNonNull()
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, equalityExpression, funWithExtensionReceiver, functionDeclaration,
ifExpression, isExpression, nullableType, operator, override, smartcast, typeConstraint, typeParameter */
