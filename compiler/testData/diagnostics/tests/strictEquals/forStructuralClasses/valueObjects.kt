// LANGUAGE: -StrictEquals +StrictEqualsForStructuralClasses +FullValueClasses
// RUN_PIPELINE_TILL: FRONTEND

value object A
value object B {
    override fun equals(other: Any?): Boolean = other is B
}
interface C
value object D : C

abstract value class E
value object F : E()

fun test(c: C, d: D, e: E): Boolean {
    if (<!EQUALITY_NOT_APPLICABLE!>A != B<!>) return true
    if (<!EQUALITY_NOT_APPLICABLE!>A == c<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE!>c == A<!>) return false
    if (c != d) return false
    if (d == c) return true
    if (D != d) return false
    if (e == F) return true
    if (F != e) return false
    if (<!EQUALITY_NOT_APPLICABLE!>A == F<!>) return false
    return true
}

fun testAfterSmartCast(d: D, nAny: Any?) {
    if (nAny == A) return
    if (A == nAny) return
    if (nAny is A && A == nAny) return
    if (nAny is B && (<!EQUALITY_NOT_APPLICABLE_WARNING!>nAny == A<!> || <!EQUALITY_NOT_APPLICABLE_WARNING!>A == nAny<!>)) return
    if (d == nAny) return
    if (nAny is C && d != nAny) return
}

interface I<U>
value object G : I<Char>

fun testGeneric(ic: I<Char>, il: I<Long>) {
    if (G == ic) return
    if (il == G) return
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, disjunctionExpression, equalityExpression, functionDeclaration,
ifExpression, interfaceDeclaration, isExpression, nullableType, objectDeclaration, operator, override, smartcast,
typeParameter, value */
