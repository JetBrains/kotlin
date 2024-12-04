// WITH_STDLIB

// FILE: 1.kt
fun interface I {
    fun f(): String

    suspend fun s() {}
}

// FILE: box.kt
fun box(): String =
    I { "OK" }.f()
