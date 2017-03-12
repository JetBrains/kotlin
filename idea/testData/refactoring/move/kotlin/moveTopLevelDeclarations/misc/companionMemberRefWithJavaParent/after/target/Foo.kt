package target

import source.Klogging

class Foo {
    companion object : Klogging()

    fun baz() {
        logger.debug { "something" }
        log("something")
    }
}