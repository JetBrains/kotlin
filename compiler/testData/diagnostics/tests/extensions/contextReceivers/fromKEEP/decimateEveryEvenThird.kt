// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters
// WITH_STDLIB

fun List<Int>.decimateEveryEvenThird() = sequence {
    var counter = 1
    for (e in this@List) {
        if (e % 2 == 0 && counter % 3 == 0) {
            yield(e)
        }
        counter += 1
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, assignment, equalityExpression, forLoop,
funWithExtensionReceiver, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral, localProperty,
multiplicativeExpression, propertyDeclaration, thisExpression */
