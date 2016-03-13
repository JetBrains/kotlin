fun box(): String {
    val result = "OK"

    fun foo() = result

    return (::foo)()
}
