package source

class Logger {
    fun debug(s: () -> String) {

    }
}

open class Klogging

val Klogging.loggerExt: Logger get() = Logger
fun Klogging.logExt(s: String) {}

