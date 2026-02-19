// RUN_PIPELINE_TILL: BACKEND
private typealias A = String.(Int) -> Int

private fun b(a: A) {
    "b".a(1)
}

fun main() {
    b {
        length + it
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
stringLiteral, typeAliasDeclaration, typeWithExtension */
