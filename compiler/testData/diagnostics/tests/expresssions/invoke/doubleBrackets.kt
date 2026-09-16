// RUN_PIPELINE_TILL: CODEGEN
fun String.k(): () -> String = { -> this }

fun test() = "hello".k()()

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral, stringLiteral,
thisExpression */
