open class Base {
}

trait Derived: <warning descr="[TRAIT_WITH_SUPERCLASS] Specifying a required base class for trait implementations is deprecated">Base</warning> {
    fun foo() {
        f1(this@Derived)
    }
}

<error descr="[UNMET_TRAIT_REQUIREMENT] Super trait 'Derived' requires subclasses to extend 'Base'">class DerivedImpl</error>(): Derived {}
<error descr="[UNMET_TRAIT_REQUIREMENT] Super trait 'Derived' requires subclasses to extend 'Base'">object ObjectImpl</error>: Derived {}

fun f1(b: Base) = b

// KT-3006