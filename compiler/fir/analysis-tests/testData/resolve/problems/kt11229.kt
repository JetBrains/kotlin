// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-11229

// KT-11229: No IMPLICIT_CAST_TO_ANY warning for Elvis branches

class A
class B
val x: A? = A()
val y: B? = B()
val z1 = if (x != null) x else y // IMPLICIT_CAST_TO_ANY in K1, no warning in K2
val z2 = x ?: y // no warning in K1 or K2

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, equalityExpression, ifExpression, nullableType,
propertyDeclaration, smartcast */
