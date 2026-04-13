// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun expandMaskConditionsAndUpdateVariableNodes(validOffsets: Collection<Int>) {}

fun main(x: List<Int>, y: Int) {
    expandMaskConditionsAndUpdateVariableNodes(
        x.mapTo(mutableSetOf()) { y }
    )
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
