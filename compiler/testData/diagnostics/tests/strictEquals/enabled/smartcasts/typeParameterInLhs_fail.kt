// RUN_PIPELINE_TILL: FRONTEND

abstract class Generic<X : Generic<X>>(val x: X) {
    abstract override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean
}

fun <T> useSite_1(g: Generic<T>) where T : Generic<T>, T : CharSequence {
    var a: Any? = null
    if (g.x == a) {
        a.x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

open class Regular {
    override fun equals(@EqualityBound(Regular::class) other: Any?): Boolean = true
}

interface Other {
    val other: String
}

fun <T> useSite_2(x: T, y: Any) where T : Regular, T : Other {
    if (x == y) {
        y.<!UNRESOLVED_REFERENCE!>other<!>
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, classReference, equalityExpression, functionDeclaration,
ifExpression, localProperty, nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast,
starProjection, typeConstraint, typeParameter */
