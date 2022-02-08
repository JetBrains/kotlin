open class GenericBaseClass<T> {
    open fun foo(x: T): T = x
    open fun ambiguous(x: T): T = x
}

interface GenericBaseInterface<T> {
    fun bar(x: T): T = x
    fun ambiguous(x: T): T = x
}

class GenericDerivedClass<T> : GenericBaseClass<T>(), GenericBaseInterface<T> {
    override fun foo(x: T): T = super.foo(x)
    override fun bar(x: T): T = super.bar(x)

    override fun ambiguous(x: T): T =
            <!AMBIGUOUS_SUPER!>super<!>.ambiguous(x)
}

class SpecializedDerivedClass : GenericBaseClass<Int>(), GenericBaseInterface<String> {
    override fun foo(x: Int): Int = super.foo(x)
    override fun bar(x: String): String = super.bar(x)

    override fun ambiguous(x: String): String =
            <!AMBIGUOUS_SUPER!>super<!>.ambiguous(x)
    override fun ambiguous(x: Int): Int =
            <!AMBIGUOUS_SUPER!>super<!>.ambiguous(x)
}

class MixedDerivedClass<T> : GenericBaseClass<Int>(), GenericBaseInterface<T> {
    override fun foo(x: Int): Int = super.foo(x)
    override fun bar(x: T): T = super.bar(x)

    override fun ambiguous(x: Int): Int =
            <!AMBIGUOUS_SUPER!>super<!>.ambiguous(x)
    override fun ambiguous(x: T): T =
            <!AMBIGUOUS_SUPER!>super<!>.ambiguous(x)
}
