private val resolve<caret>Me: A = null = <expr>object : A<Int> {
    override fun x() {}
}</expr>

interface A<T> {
    fun x()
}
