fun box(): String {
    fun foo(): String {
        fun bar() = "OK"
        val ref = ::bar
        return ref()
    }

    val ref = ::foo
    return ref()
}
