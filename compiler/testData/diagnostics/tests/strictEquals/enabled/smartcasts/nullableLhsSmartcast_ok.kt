// RUN_PIPELINE_TILL: BACKEND
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
}

fun useSite_1(x: Regular?, y: Any?) {
    if (x == y) {
        y?.p?.length
        y?.q?.length
    }
}

fun <T : Regular?> useSite_2(x: T, y: Parent?) {
    if (x == y) {
        y?.p?.length
        y?.q?.length
    }
}

fun useSite_3(x: Regular?, y: Parent) {
    if (x == y) {
        y.p.length
    }
}

fun <T : Regular> useSite_4(x: T?, y: Parent?) {
    if (x == y) {
        y?.p?.length
    }
}

fun <T : Regular, S : Parent> useSite_5(x: T?, y: S) {
    if (x == y) {
        y.p.length
        y.q.length
    }
}

fun useSite_6(x: Other?, y: Any?) {
    if (x == y) {
        y?.q?.length
    }
}

fun <T: Other, S: Any> useSite_7(x: T?, y: S?) {
    if (x == y) {
        y?.q?.length
    }
}

fun <T: Other?> useSite_8(x: T, y: Any?) {
    if (x == y) {
        y?.q?.length
    }
}

fun <T: Other?> useSite_9(x: T?, y: Any?) {
    if (x == y) {
        y?.q?.length
    }
}

fun <T: Other?> useSite_10(x: T?, y: Any) {
    if (x != y) return
    y.q.length
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, getter, ifExpression,
interfaceDeclaration, intersectionType, nullableType, operator, override, propertyDeclaration, safeCall, smartcast,
typeConstraint, typeParameter */
