// RUN_PIPELINE_TILL: BACKEND
fun box(): String = "OK"

fun main(args: Array<String>) {
    if (box() == "OK") {
        throw Exception("Hello")
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, stringLiteral */
