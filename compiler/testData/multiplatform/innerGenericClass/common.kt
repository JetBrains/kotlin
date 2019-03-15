class A<E> {
    inner class B<T, E> {
        fun getAE() = this@A.getAE()
        fun getBT(): T? = null
        fun getBE(): E? = null
    }

    fun getAE(): E? = null
}
