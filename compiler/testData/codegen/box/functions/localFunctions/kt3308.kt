fun box(): String {
    fun <T> foo(t: T) = t

    return foo("OK")
}