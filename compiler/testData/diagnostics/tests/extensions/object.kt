// RUN_PIPELINE_TILL: BACKEND
object O

fun Any.foo() = 42
val Any?.bar: Int get() = 239

val x = O.foo() + O.bar

/* GENERATED_FIR_TAGS: additiveExpression, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
nullableType, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver */
