// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-84061
// WITH_STDLIB

fun main() {
    buildList {
        add("O")
        this[0] += "K"
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral, thisExpression */
