open class Base {
}

interface Derived: <!TRAIT_WITH_SUPERCLASS!>Base<!> {
    fun foo() {
        f1(this@Derived)
    }
}

<!UNMET_TRAIT_REQUIREMENT!>class DerivedImpl<!>(): Derived {}
<!UNMET_TRAIT_REQUIREMENT!>object ObjectImpl<!>: Derived {}

fun f1(b: Base) = b
