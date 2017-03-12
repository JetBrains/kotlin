package source

class Logger {
    fun debug(s: () -> String) {

    }
}

interface ILogging {
    val logger: Logger
    fun log(s: String)
}

class Klogging : ILogging {
    override val logger = Logger()
    override fun log(s: String) {}
}

