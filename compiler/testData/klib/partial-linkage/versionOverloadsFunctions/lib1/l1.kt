class C {
    fun Int.foo(): String = "$this"

    suspend fun bar(a: Int = 1): String = "bar/$a"
}