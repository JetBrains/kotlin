package target

import source.Klogging
import source.logExt
import source.loggerExt

class Foo {
    companion object : Klogging()

    fun baz() {
        loggerExt.debug { "something" }
        logExt("something")
    }
}