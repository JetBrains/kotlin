// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

data class Language(var name: String)

interface LoggingContext {
    fun log(level: Int, message: String)
}

interface SaveRepository<T> {
    context(LoggingContext)
    fun save(content: T)
}

context(LoggingContext, SaveRepository<Language>)
fun startBusinessOperation() {
    log(0, "Operation has started")
    save(Language("Kotlin"))
}

class CompositeContext(c1: LoggingContext, c2: SaveRepository<Language>): LoggingContext by c1, SaveRepository<Language> by c2

fun box(): String {
    val loggingCtx = object : LoggingContext {
        override fun log(level: Int, message: String) {}
    }

    val saveCtx = object : SaveRepository<Language> {
        context(LoggingContext)
        override fun save(content: Language) {
            log(message = "Saving $content", level = 123)
        }
    }

    with(CompositeContext(loggingCtx, saveCtx)) {
        startBusinessOperation()
    }

    return "OK"
}
