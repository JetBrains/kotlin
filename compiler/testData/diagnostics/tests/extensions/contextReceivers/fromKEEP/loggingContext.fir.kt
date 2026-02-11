// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

interface Params
interface Logger {
    fun info(message: String)
}
interface LoggingContext {
    val log: Logger // this context provides reference to logger
}

context(LoggingContext)
fun performSomeBusinessOperation(withParams: Params) {
    <!UNRESOLVED_REFERENCE!>log<!>.info("Operation has started")
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, propertyDeclaration,
stringLiteral */
