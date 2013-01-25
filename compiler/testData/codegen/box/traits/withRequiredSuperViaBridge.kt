open class Base {
    open fun foo() { }
}

trait Derived : Base {
    override fun foo() {
        object {
            fun bar() {
                super<Base>@Derived.foo()
            }
        }.bar()
    }
}

class DerivedImpl : Derived, Base()

fun box(): String {
    DerivedImpl().foo()
    return "OK"
}
