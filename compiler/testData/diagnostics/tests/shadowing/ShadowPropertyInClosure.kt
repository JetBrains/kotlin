// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
val i = 17

val f: () -> Int = { var i = 17; i }

/* GENERATED_FIR_TAGS: functionalType, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration */
