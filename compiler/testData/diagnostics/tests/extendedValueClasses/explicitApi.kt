// LANGUAGE: +ValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// EXPLICIT_API_MODE: STRICT

public value class FooValue(val i: Int)
public value class FooValue_(val i: Int, val i1: Int)
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>value class FooValue2<!>(val i: Int)
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>value class FooValue2_<!>(val i: Int, val i1: Int)
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>sealed value class FooValue3<!>(i: Int)
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>abstract value class FooValue4<!>(i: Int)
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!><!VALUE_CLASS_OPEN!>open<!> value class FooValue5<!>(i: Int)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, value */
