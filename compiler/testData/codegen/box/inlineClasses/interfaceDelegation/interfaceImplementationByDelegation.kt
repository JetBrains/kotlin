// WITH_RUNTIME

interface IFoo {
    fun getO(): String
    val k: String

    val ok: String get() = getO() + k
}

@JvmInline
value class InlineFooImpl(val s: String): IFoo {
    override fun getO(): String = s
    override val k: String get() = "K"
}

class Test(s: String) : IFoo by InlineFooImpl(s)

fun box() = Test("O").ok