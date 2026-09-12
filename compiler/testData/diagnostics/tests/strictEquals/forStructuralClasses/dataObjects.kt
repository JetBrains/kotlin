// LANGUAGE: -StrictEquals +StrictEqualsForStructuralClasses
// RUN_PIPELINE_TILL: BACKEND

data object A
data class B(val y: Int) {
    override fun equals(other: Any?): Boolean = other is B
}
sealed class C
data object D : C()

fun test(b: B, c: C, d: D): Boolean {
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>A != b<!>) return true
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>A == c<!>) return false
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>c == A<!>) return false
    if (c != d) return false
    if (d == c) return true
    if (D != d) return false
    return true
}

/* GENERATED_FIR_TAGS: classDeclaration, data, equalityExpression, functionDeclaration, ifExpression, isExpression,
nullableType, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, sealed, smartcast */
