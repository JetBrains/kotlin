interface A<R>

interface B {
    fun accept(visitor: String)
    fun <R> accept(visitor: A<R>): R
}

interface C : B {
    override fun <R> accept(visitor: A<R>): R
}
