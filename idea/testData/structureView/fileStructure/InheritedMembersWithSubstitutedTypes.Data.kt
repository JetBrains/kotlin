trait First<T> {
    fun foo(a: T)
}

trait Second<U> : First<Int> {
    val a: U
}