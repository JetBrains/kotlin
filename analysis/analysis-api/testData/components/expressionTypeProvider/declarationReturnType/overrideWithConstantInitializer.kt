// WITH_STDLIB
interface Base {
    val long: Long
    val any: Any
    val getter: Long
    fun longFunction(): Long
    fun anyFunction(): Any
}

class Derived : Base {
    override val long = 1
    override val any = 1
    override val getter get() = 1
    override fun longFunction() = 1
    override fun anyFunction() = 1
}
