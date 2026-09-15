// LANGUAGE: -StrictEquals +StrictEqualsForStructuralClasses
// RUN_PIPELINE_TILL: BACKEND

data object A
data class B(val y: Int) {
    override fun equals(other: Any?): Boolean = other is B
}
sealed class C
data object D : C()

fun test(b: B, c: C, d: D): Boolean {
    if (A != b) return true
    if (A == c) return false
    if (c == A) return false
    if (c != d) return false
    if (d == c) return true
    if (D != d) return false
    return true
}

/* GENERATED_FIR_TAGS: classDeclaration, data, equalityExpression, functionDeclaration, ifExpression, isExpression,
nullableType, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, sealed, smartcast */
