// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitIllegalNotNullSmartCastsInEqualities

interface Parent {
    val q: String get() = "!"
}

open class Regular : Parent {
    override fun equals(@EqualityBound(Regular::class) other: Any?): Boolean = true
    val p: String get() = "!"
}

open class Other : Parent {
    override fun equals(@EqualityBound(Parent::class) other: Any?): Boolean = true
    val r: String get() = "!"
}

fun useSite_1(x: Regular?, y: Any?) {
    if (x == y) {
        y<!UNSAFE_CALL!>.<!>p.length
        y<!UNSAFE_CALL!>.<!>q.length
    }
}

fun <T : Regular?> useSite_2(x: T, y: Parent?) {
    if (x == y) {
        y<!UNSAFE_CALL!>.<!>p.length
        y<!UNSAFE_CALL!>.<!>q.length
    }
}

fun <T : Regular> useSite_3(x: T?, y: Parent?) {
    if (x == y) {
        y<!UNSAFE_CALL!>.<!>p.length
    }
}

fun useSite_4(x: Other?, y: Any?) {
    if (x == y) {
        y<!UNSAFE_CALL!>.<!>q.length
        y?.<!UNRESOLVED_REFERENCE!>r<!>?.length
    }
}

fun <T: Other?> useSite_5(x: T?, y: Any) {
    if (x != y) return
    y<!UNNECESSARY_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>r<!>?.length
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, getter, ifExpression,
interfaceDeclaration, intersectionType, nullableType, operator, override, propertyDeclaration, safeCall, smartcast,
typeConstraint, typeParameter */
