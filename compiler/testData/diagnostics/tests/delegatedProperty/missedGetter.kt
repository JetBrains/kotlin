// RUN_PIPELINE_TILL: FRONTEND
val a: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING("getValue(Nothing?, KProperty<*>); A; delegate")!>A()<!>

class A

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration, propertyDelegate, starProjection */
