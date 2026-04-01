// LANGUAGE: +ValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// EXPLICIT_API_MODE: STRICT

public value class FooValue(val i: Int)
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>value class FooValue2<!>(val i: Int)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, value */
