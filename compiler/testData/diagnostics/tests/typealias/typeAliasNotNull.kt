// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
typealias Processor = (number: Int) -> Unit
typealias Handler = (String) -> Processor?

fun x(handler: Handler) {
    return handler("a")!!(1)
}
