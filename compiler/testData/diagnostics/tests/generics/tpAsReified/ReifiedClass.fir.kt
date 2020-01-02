class A<T1, reified T2, T3, reified T4> {
    fun<reified R> foo(): T2 = throw UnsupportedOperationException()
}
