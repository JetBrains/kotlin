// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// Public property with explicit type
val a: String = "A"
// Public property with implicit type
val b = "B"
// Property with overriden getter and implicit type.
fun foo() = "C"
val c get() = foo()

/* GENERATED_FIR_TAGS: propertyDeclaration, stringLiteral */
