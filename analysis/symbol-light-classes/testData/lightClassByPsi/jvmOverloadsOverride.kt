// LIBRARY_PLATFORMS: JVM

// `@JvmOverloads` overloads belong to the declaration that *declares* the default values.
// `Base.foo` must expand into foo(), foo(Int), foo(Int, Int), foo(Int, Int, Int), while the override
// `Derived.foo` must NOT generate any overloads even though it inherits the default values:
// the compiler reports OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS for it and emits only the full method.
open class Base {
    @JvmOverloads
    open fun foo(p1: Int = 0, p2: Int = 0, p3: Int = 0) {}
}

class Derived : Base() {
    @Suppress("OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS")
    @JvmOverloads
    override fun foo(p1: Int, p2: Int, p3: Int) {}
}
