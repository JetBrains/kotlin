package test

class P {
    private val FOO_PRIVATE = "OK"

    final val FOO_FINAL = "OK"

    private inline fun fooPrivate(): String {
        return FOO_PRIVATE
    }

    private inline fun fooFinal(): String {
        return FOO_FINAL
    }

    fun testPrivate(): String {
        return fooPrivate()
    }

    fun testFinal(): String {
        return fooFinal()
    }
}