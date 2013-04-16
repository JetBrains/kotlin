open class Base {
    open fun foo() { }
    open fun foo2() { }
}

trait Derived : Base {
    override fun foo() {
        object {
            fun bar() {
                //super<Base>@Derived.foo2()
                this@Derived.foo2()
            }
        }.bar()
    }
}

class DerivedImpl : Derived, Base()

fun box(): String {
    DerivedImpl().foo()
    return "OK"
}
