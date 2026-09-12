// LANGUAGE: -StrictEquals +StrictEqualsForStructuralClasses
// RUN_PIPELINE_TILL: BACKEND

annotation class A(val x: Int)
annotation class B(val y: Int)
interface C
annotation class G<T>(val t: Int)

fun test(a: A, b: B, c: C, ann: Annotation, any: Any): Boolean {
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>a != b<!>) return true
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>a == c<!>) return false
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>c == a<!>) return false
    if (a == ann) return false
    if (ann != a) return false
    if (a == any) return true
    return true
}

fun testAfterSmartCast(a: A, nAny: Any?) {
    if (nAny == a) return
    if (a == nAny) return
    if (nAny is A && a == nAny) return
    if (nAny is B && (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>nAny == a<!> || <!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>a == nAny<!>)) return
    if (nAny is Annotation && a != nAny) return
    if (nAny is C && <!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>a == nAny<!>) return
}

fun testGeneric(gi: G<Int>, gi2: G<Int>, gs: G<String>, ann: Annotation) {
    if (gi == gi2) return
    if (gi2 != gi) return
    if (gs == gi) return
    if (gs != ann) return
}

/* GENERATED_FIR_TAGS: andExpression, annotationDeclaration, disjunctionExpression, equalityExpression,
functionDeclaration, ifExpression, interfaceDeclaration, intersectionType, isExpression, nullableType,
primaryConstructor, propertyDeclaration, smartcast, typeParameter */
