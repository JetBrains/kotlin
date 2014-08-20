open class Base {
}

trait Derived: Base {
    fun foo() {
        f1(this@Derived)
    }
}

class <!UNMET_TRAIT_REQUIREMENT!>DerivedImpl()<!>: Derived {}
object <!UNMET_TRAIT_REQUIREMENT!>ObjectImpl<!>: Derived {}

fun f1(b: Base) = b
