open class Base

interface Derived : Base {
    fun foo(): String {
        return object {
            fun bar() = baz(this@Derived)
        }.bar()
    }
}

class DerivedImpl : Derived, Base()

fun baz(b: Base) = "OK"

fun box() = DerivedImpl().foo()
