// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses

value class A(val a: B?, val i: Int)

value class B(val b: A)

value class E(val e: F?)

value class F(val f: E?, val i: Int)

value class S(val s: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>T?<!>)

value class T(val t: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>S<!>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, value */
