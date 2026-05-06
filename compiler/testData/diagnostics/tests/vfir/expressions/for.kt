// RUN_PIPELINE_TILL: BACKEND
fun main() {
    var v = 0
    for(i in 1..3) {
        v += 1
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, forLoop, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, rangeExpression */
