// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-51143

fun main() {
    buildMap {
        if (true) {
            println("test")
        } else {
            put("foo", "bar")
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, lambdaLiteral, nullableType, stringLiteral */
