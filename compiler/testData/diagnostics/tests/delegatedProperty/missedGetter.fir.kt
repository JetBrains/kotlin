// RUN_PIPELINE_TILL: FRONTEND
val a: Int <!DELEGATION_OPERATOR_MISSING("getValue(Nothing?, KProperty0<*>); A; delegate")!>by<!> A()

class A

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration, propertyDelegate, starProjection */
