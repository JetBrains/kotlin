package source

class Logger {
    fun debug(s: () -> String) {

    }
}

open class Klogging

val Klogging.loggerExt: Logger get() = Logger
fun Klogging.logExt(s: String) {}

class <caret>Foo {
    companion object : Klogging()

    fun baz() {
        loggerExt.debug { "something" }
        logExt("something")
    }
}