interface A {
    fun a(): A
}

fun error(s: String): Nothing = null!!

class A1 : A {
    override fun a() = test() ?: error("")

    @Suppress("UNCHECKED_CAST")
    private fun <V : A> test(): V? = this as? V
}
