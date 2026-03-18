// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-19143
// WITH_STDLIB

// KT-19143: Int.invoke() interferes with IntProgression.step()
operator fun Int.invoke(i: Int) = this * i

fun main(args: Array<String>) {
    for (i in 0..5) { println(i) }
    for (i in 0..5 step 3) { println(i) }
}

/* GENERATED_FIR_TAGS: forLoop, funWithExtensionReceiver, functionDeclaration, integerLiteral, localProperty,
multiplicativeExpression, operator, propertyDeclaration, rangeExpression, thisExpression */
