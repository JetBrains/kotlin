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
    override fun ambiguous(x: T): T = foo(x)
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>class SpecializedDerivedClass : GenericBaseClass<Int>(), GenericBaseInterface<String> {
    override fun foo(x: Int): Int = super.foo(x)
    override fun bar(x: String): String = super.bar(x)
    override fun ambiguous(x: String): String = bar(x)
    override fun ambiguous(x: Int): Int = foo(x)
}<!>

class MixedDerivedClass<T> : GenericBaseClass<Int>(), GenericBaseInterface<T> {
    override fun foo(x: Int): Int = super.foo(x)
    override fun bar(x: T): T = super.bar(x)
    override fun ambiguous(x: Int): Int = foo(x)
    <!ACCIDENTAL_OVERRIDE!>override fun ambiguous(x: T): T = bar(x)<!>
}
