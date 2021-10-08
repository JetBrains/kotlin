// WITH_RUNTIME

abstract class GenericBase<T> {
    abstract fun foo(x: T): T
}

@JvmInline
value class Str(val str: String)

class Derived : GenericBase<Str>() {
    override fun foo(x: Str): Str = x
}

fun box() = Derived().foo(Str("OK")).str