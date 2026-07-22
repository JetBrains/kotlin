// RUN_PIPELINE_TILL: FRONTEND

open class B : A<String>() {
    override fun equals(other: Any?): Boolean = true
}

class F : I, A<String>() {
    override fun equals(other: Any?): Boolean = true
}

open class A<X> : I {
    val x: X? get() = null
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

open class D : C() {
    override fun equals(other: Any?): Boolean = true
}

open class C : B()
open class E : D()

interface I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

fun useSites(a: Any?, b: B, c: C, d: D, e: E, f: F) {
    if (b == a) {
        a.x
        a.x?.<!UNRESOLVED_REFERENCE!>length<!>
    }
    if (c == a) {
        a.x
        a.x?.<!UNRESOLVED_REFERENCE!>length<!>
    }
    if (d == a) {
        a.x
        a.x?.<!UNRESOLVED_REFERENCE!>length<!>
    }
    if (e == a) {
        a.x
        a.x?.<!UNRESOLVED_REFERENCE!>length<!>
    }
    if (f == a) {
        a.x
        a.x?.<!UNRESOLVED_REFERENCE!>length<!>
    }

    open class G : E() {
        override fun equals(@EqualityBound(B::class) other: Any?): Boolean = true
        inner class H : G() {
            override fun equals(other: Any?): Boolean = true
        }
    }

    if (G().H() == a) {
        a.x
        a.x?.length
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, getter, ifExpression,
interfaceDeclaration, nullableType, operator, override, propertyDeclaration, safeCall, smartcast, typeParameter */
