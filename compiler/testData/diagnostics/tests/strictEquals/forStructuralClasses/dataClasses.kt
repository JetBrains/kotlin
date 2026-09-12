// LANGUAGE: -StrictEquals +StrictEqualsForStructuralClasses
// RUN_PIPELINE_TILL: BACKEND

data class A(val x: Int)
data class B(val y: Int) {
    override fun equals(other: Any?): Boolean = other is B
}
interface C
data class D(val t: Int) : C

fun test(a: A, b: B, c: C, d: D, d2: D): Boolean {
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>a != b<!>) return true
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>a == c<!>) return false
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>c == a<!>) return false
    if (c != d) return false
    if (d == c) return true
    if (d2 != d) return false
    return true
}

fun testAfterSmartCast(a: A, d: D, nAny: Any?) {
    if (nAny == a) return
    if (a == nAny) return
    if (nAny is A && a == nAny) return
    if (nAny is B && (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>nAny == a<!> || <!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>a == nAny<!>)) return
    if (d == nAny) return
    if (nAny is C && d != nAny) return
}

interface I<U>
data class G<T>(val t: T) : I<Char>

fun testGeneric(
    gi: G<Int>,
    gi2: G<Int>,
    gs: G<String>,
    ic: I<Char>,
    il: I<Long>,
) {
    if (gi == gi2) return
    if (gi2 != gi) return
    if (gs == gi) return
    if (gs != ic) return
    if (il == gs || gi == il) return
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, data, disjunctionExpression, equalityExpression,
functionDeclaration, ifExpression, interfaceDeclaration, isExpression, nullableType, primaryConstructor,
propertyDeclaration, smartcast, typeParameter */
