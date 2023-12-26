// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// JVM_ABI_K1_K2_DIFF: KT-63828
interface IFoo {
    fun getO(): String
    val k: String

    val ok: String get() = getO() + k
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineFooImpl(val s: String): IFoo {
    override fun getO(): String = s
    override val k: String get() = "K"
}

class Test(s: String) : IFoo by InlineFooImpl(s)

fun box() = Test("O").ok