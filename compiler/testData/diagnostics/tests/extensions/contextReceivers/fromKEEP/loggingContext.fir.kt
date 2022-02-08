// !LANGUAGE: +ContextReceivers

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