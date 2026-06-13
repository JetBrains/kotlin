class MyException : Exception()

expect interface Closeable {
    fun close()
}

interface SdkSink : Closeable {
    @Throws(MyException::class)
    override fun close()
}
