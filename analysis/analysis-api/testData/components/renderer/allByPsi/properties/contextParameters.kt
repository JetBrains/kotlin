// LANGUAGE: +ContextParameters

class Logger

context(logger: Logger)
val contextValue: Int
    get() = 1

context(logger: Logger, _: String)
var contextVariable: Int
    get() = 1
    set(value) {}
