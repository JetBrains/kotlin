// RUN_PIPELINE_TILL: CODEGEN
private const val a = ""

@Deprecated("$a")
fun test() {}

/* GENERATED_FIR_TAGS: const, functionDeclaration, propertyDeclaration, stringLiteral */
