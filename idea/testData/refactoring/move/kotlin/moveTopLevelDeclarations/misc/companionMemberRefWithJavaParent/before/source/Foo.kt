package source

class Logger {
    fun debug(s: () -> String) {

    }
}

class <caret>Foo {
    companion object : Klogging()

    fun baz() {
        logger.debug { "something" }
        log("something")
    }
}