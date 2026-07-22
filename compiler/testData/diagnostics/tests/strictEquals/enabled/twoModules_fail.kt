// RUN_PIPELINE_TILL: FRONTEND

// MODULE: lib

// FILE: A.kt

open class A<T> {
    val r: T? get() = null
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

// MODULE: main(lib)

// FILE: Main.kt

class C : A<String>()

class D : A<String>() {
    override fun equals(other: Any?): Boolean = other.r?.<!UNRESOLVED_REFERENCE!>length<!> == null
}

class E : A<String>() {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = other.r?.<!UNRESOLVED_REFERENCE!>length<!> == null
}

fun useSite_1(x: Any?) {
    if (C() == x) {
        x.r?.<!UNRESOLVED_REFERENCE!>length<!>
    }
    if (D() == x) {
        x.r!!.<!UNRESOLVED_REFERENCE!>length<!>
    }
    if (E() == x) {
        x.r.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, classReference, equalityExpression,
functionDeclaration, getter, ifExpression, nullableType, operator, override, propertyDeclaration, safeCall, smartcast,
starProjection, typeParameter */
