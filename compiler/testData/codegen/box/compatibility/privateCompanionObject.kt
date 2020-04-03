class Test {
    private companion object {
        val res = "OK"
    }
    fun res() = res
}

fun box(): String {
    return Test().res()
}
