open class Base {
    open fun foo() : String {
      return "fail"
    }
}

trait Derived : Base {
    override fun foo() : String {
        //super.foo()
        return "OK"
    }
}

class DerivedImpl : Derived, Base()

fun box(): String {
    return DerivedImpl().foo()
}
