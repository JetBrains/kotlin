// WITH_STDLIB

fun interface I {
    fun f(): String

    suspend fun s() {}
}

fun box(): String =
    I { "OK" }.f()
