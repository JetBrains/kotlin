// RUN_PIPELINE_TILL: FRONTEND
class A<T> : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>T<!> {}
class B<T> : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>T & Any<!> {}

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, typeParameter */
