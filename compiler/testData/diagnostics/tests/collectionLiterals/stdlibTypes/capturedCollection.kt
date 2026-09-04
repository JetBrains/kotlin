// RUN_PIPELINE_TILL: BACKEND

fun MutableList<in List<Int>>.addCL() {
    add([])
    add([1, 2, 3])
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, inProjection, integerLiteral */
