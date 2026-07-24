// RUN_PIPELINE_TILL: FRONTEND

class Generic<T>(val t: T) {
    override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean = true

    inner class Inner(val p: T) {
        override fun equals(@EqualityBound(Inner::class) other: Any?): Boolean = true
    }
}

fun Generic<String>.useSite_1(other: Any?): Int {
    return if (this == other) {
        other.t.<!UNRESOLVED_REFERENCE!>length<!>
    } else if (Inner(t) == other) {
        other.p.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        0
    }
}

open class Inheritable {
    override fun equals(@EqualityBound(Inheritable::class) other: Any?): Boolean = true
}

class Inherited(val p: String) : Inheritable() {
    fun useSite_2(other: Any?): Int {
        return if (this == other) other.<!UNRESOLVED_REFERENCE!>p<!>.length else 0
    }

    fun useSite_3(other: Inheritable?): Int {
        return if (this == other) other.<!UNRESOLVED_REFERENCE!>p<!>.length else 0
    }
}

open class Parent {
    override fun equals(@EqualityBound(Parent::class) other: Any?): Boolean = true
}

class Child(val p: String) : Parent() {
    override fun equals(@EqualityBound(Child::class) other: Any?): Boolean = true
}

fun useSite_4(p1: Parent, p2: Child) {
    if (p1 == p2) {
        p1.<!UNRESOLVED_REFERENCE!>p<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, funWithExtensionReceiver,
functionDeclaration, ifExpression, inner, integerLiteral, nullableType, operator, override, primaryConstructor,
propertyDeclaration, smartcast, thisExpression, typeParameter */
