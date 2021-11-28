// WITH_STDLIB

interface A {
    fun f(x: String) = x
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class B(val y: String) : A {
    override fun f(x: String) = super.f(x + y)
}

fun box(): String {
    return B("K").f("O")
}
