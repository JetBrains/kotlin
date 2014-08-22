open class Base {
}

trait Derived: Base {
    fun foo() {
        f1(this@Derived)
    }
}

class <error descr="[UNMET_TRAIT_REQUIREMENT] Super trait 'Derived' requires subclasses to extend 'Base'">DerivedImpl()</error>: Derived {}
object <error descr="[UNMET_TRAIT_REQUIREMENT] Super trait 'Derived' requires subclasses to extend 'Base'">ObjectImpl</error>: Derived {}

fun f1(b: Base) = b

// KT-3006