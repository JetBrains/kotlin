// DIAGNOSTICS: -UNUSED_PARAMETER

interface IBase {
    fun foo()
    val bar: Int
}

object Impl : IBase {
    override fun foo() {}
    override val bar: Int get() = 42
}

object Test : IBase by Impl

fun Test.foo() {}
val Test.bar: Int get() = 42