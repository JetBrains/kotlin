class MyException : Exception()

expect interface Closeable {
    fun close()
}

interface SdkSink : Closeable {
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws(MyException::class)<!>
    override fun close()
}
