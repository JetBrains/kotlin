// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57839

fun <R> myRun(block: () -> R): R {
    return block()
}

interface Bar {
    val action: () -> Unit
}

val cardModel = myRun {
    object : Bar {
        override val action = {}
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, nullableType, override, propertyDeclaration, typeParameter */
