fun box(): String {
    fun foo<T>(t: T) = t

    return foo("OK")
}