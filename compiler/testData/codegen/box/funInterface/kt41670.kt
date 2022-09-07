fun interface Interface {
    fun foo()

    val value get() = "OK"
}

fun box() = Interface{}.value