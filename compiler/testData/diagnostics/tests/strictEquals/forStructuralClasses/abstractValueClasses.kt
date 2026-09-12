// LANGUAGE: -StrictEquals +StrictEqualsForStructuralClasses +FullValueClasses
// RUN_PIPELINE_TILL: BACKEND

abstract value class A
abstract value class B {
    final override fun equals(other: Any?): Boolean = other is B
}
sealed value class S
interface C
abstract value class D : C

value class E(val x: Int) : A()
value class F(val y: Int)

fun test(a: A, b: B, s: S, c: C, d: D, e: E, e2: E, f: F): Boolean {
    if (a != b) return true
    if (a == s) return false
    if (a == c) return false
    if (c == a) return false
    if (a == e) return true
    if (e2 != e) return false
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>a == f<!>) return false
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>f != b<!>) return false
    if (d == c) return true
    if (c != d) return false
    return true
}

fun testAfterSmartCast(a: A, d: D, nAny: Any?) {
    if (a == nAny) return
    if (nAny == a) return
    if (nAny is A && a == nAny) return
    if (nAny is B && a == nAny) return
    if (nAny is F && <!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>a == nAny<!>) return
    if (nAny is C && d != nAny) return
}

interface I<U>
abstract value class G<T> : I<Char>
value class H(val t: Int) : G<Int>()

fun testGeneric(gi: G<Int>, gi2: G<Int>, gs: G<String>, ic: I<Char>, il: I<Long>, h: H) {
    if (gi == gi2) return
    if (gi2 != gi) return
    if (gs == gi) return
    if (gs != ic) return
    if (il == gs || gi == il) return
    if (h == gi) return
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, disjunctionExpression, equalityExpression, functionDeclaration,
ifExpression, interfaceDeclaration, isExpression, nullableType, operator, override, primaryConstructor,
propertyDeclaration, sealed, smartcast, typeParameter, value */
