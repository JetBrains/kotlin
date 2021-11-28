// WITH_STDLIB

abstract class GenericBase<T> {
    abstract fun foo(x: T): T
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Str(val str: String)

class Derived : GenericBase<Str>() {
    override fun foo(x: Str): Str = x
}

fun box() = Derived().foo(Str("OK")).str