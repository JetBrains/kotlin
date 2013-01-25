open class Base {
    open fun foo() { }
}

trait Derived : Base {
    override fun foo() {
        super.foo()
    }
}

class DerivedImpl : Derived, Base()

fun box(): String {
    DerivedImpl().foo()
    return "OK"
}
