// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class MyClass<T: <!CYCLIC_GENERIC_UPPER_BOUND!>T?<!>>

/* GENERATED_FIR_TAGS: classDeclaration, typeConstraint, typeParameter */
