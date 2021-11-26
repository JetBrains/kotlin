// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IC(val x: String)

interface I<T> {
    fun foo(): T
}

interface II: I<IC>

class A : I<IC> {
    override fun foo() = IC("O")
}

class B : II {
    override fun foo() = IC("K")
}

fun box() = A().foo().x + B().foo().x
