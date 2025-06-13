// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
typealias Processor = (number: Int) -> Unit
typealias Handler = (String) -> Processor?

fun x(handler: Handler) {
    return handler("a")!!(1)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, functionalType, integerLiteral, nullableType,
stringLiteral, typeAliasDeclaration */
