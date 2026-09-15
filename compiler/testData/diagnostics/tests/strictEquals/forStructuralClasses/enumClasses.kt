// LANGUAGE: -StrictEquals +StrictEqualsForStructuralClasses
// RUN_PIPELINE_TILL: FRONTEND

enum class A { X, Y }
enum class B { X, Y }
interface C
enum class D : C { X, Y }

class E(val x: Int) {
    override fun equals(other: Any?): Boolean = other is E
    override fun hashCode(): Int = x
}

fun test(a: A, b: B, c: C, d: D, d2: D, e: E): Boolean {
    if (<!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>a != b<!>) return true
    if (<!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>a == c<!>) return false
    if (<!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>c == a<!>) return false
    if (c != d) return false
    if (d == c) return true
    if (d2 != d) return false
    if (<!EQUALITY_NOT_APPLICABLE!>a == e<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE!>e != a<!>) return false
    return true
}

fun testNullable(an: A?, bn: B?, b: B): Boolean {
    if (<!INCOMPATIBLE_ENUM_COMPARISON!>an == bn<!>) return true
    if (<!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>an != b<!>) return false
    return true
}

fun testAfterSmartCast(a: A, d: D, nAny: Any?) {
    if (nAny == a) return
    if (a == nAny) return
    if (nAny is A && a == nAny) return
    if (nAny is B && (<!INCOMPATIBLE_ENUM_COMPARISON!>nAny == a<!> || <!INCOMPATIBLE_ENUM_COMPARISON!>a == nAny<!>)) return
    if (d == nAny) return
    if (nAny is C && d != nAny) return
}

interface I<U>
enum class G : I<Char> { X, Y }

fun testGeneric(ic: I<Char>, il: I<Long>, g: G) {
    if (g == ic) return
    if (ic != g) return
    if (il == g) return
    if (G.X == il) return
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, disjunctionExpression, enumDeclaration, enumEntry,
equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration, intersectionType, isExpression,
nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast, typeParameter */
