
class MyException : Exception()
class C @Throws(Exception::class) constructor(a: Int = 1) {
    @Throws(java.io.IOException::class, MyException::class)
    fun readFile(name: String): String { TODO() }

    // Should be mapped to java.lang.Throwable
    @Throws(kotlin.Throwable::class)
    fun baz() {}
}
