// FIR_IDENTICAL
// ISSUE: KT-67593

interface Foo {
    fun bar(x: Int, y: String? = null): String
}

open class FooFoo(val f: Foo) : Foo by f

class Final(f: Foo) : FooFoo(f) {
    override fun bar(x: Int, y: String?): String {
        return super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>bar<!>(x)
    }
}

interface Generic<T> {
    fun bar(x: Int = 0)
}

open class Impl(val g: Generic<String>) : Generic<String> by g

class Final2(g: Generic<String>) : Impl(g) {
    override fun bar(x: Int) {
        return super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>bar<!>()
    }
}

open class GenericClass<T> {
    open fun bar(x: Int = 0) {}
}

class FinalClass : GenericClass<String>() {
    override fun bar(x: Int) {
        return super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>bar<!>()
    }
}

open class A {
    open fun bar(x: Int = 0) {}

    open fun baz(x: Int) {}
}

interface B {
    fun bar(x: Int)

    fun baz(x: Int = 0)
}

class AB : A(), B {
    override fun bar(x: Int) {
        return super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>bar<!>()
    }

    override fun baz(x: Int) {
        return super.baz<!NO_VALUE_FOR_PARAMETER!>()<!>
    }
}
