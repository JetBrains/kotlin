// RUN_PIPELINE_TILL: CODEGEN
@Volatile
var xx: Int = 2

@Synchronized
fun foo() {}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, propertyDeclaration */
