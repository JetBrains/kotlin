// RUN_PIPELINE_TILL: CODEGEN
const val myPi = kotlin.math.PI

annotation class Anno(val d: Double)

@Anno(kotlin.math.PI)
fun f() {}

@Anno(myPi)
fun g() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, const, functionDeclaration, primaryConstructor, propertyDeclaration */
