// RUN_PIPELINE_TILL: CODEGEN
val i = 17

val f: () -> Int = { var i = 17; i }

/* GENERATED_FIR_TAGS: functionalType, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration */
