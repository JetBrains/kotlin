// RUN_PIPELINE_TILL: BACKEND

// MODULE: a
// FILE: A.kt
interface A {
    fun a(): Boolean = true
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

// MODULE: b(a)
// FILE: B.kt
open class B : A {
    override fun equals(other: Any?): Boolean = other.a()
}

abstract class C : A

// MODULE: c(a, b)
// FILE: C.kt

class D : B()

class E : C() {
    override fun equals(other: Any?): Boolean = other.a()
}

fun useSite_1(any: Any?, a: A, b: B, c: C, d: D, e: E) {
    if (a == any) any.a()
    if (b == any) any.a()
    if (c == any) any.a()
    if (d == any) any.a()
    if (e == any) any.a()
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, nullableType,
operator, override, smartcast */
