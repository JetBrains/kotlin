fun box(): String {
    fun foo() = "OK"
    return (::foo)()
}
