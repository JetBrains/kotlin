open class Base {
}

trait Derived: Base {
    fun foo() {
        f1(this@Derived)
    }
}

<!UNMET_TRAIT_REQUIREMENT!>class DerivedImpl<!>(): Derived {}
<!UNMET_TRAIT_REQUIREMENT!>object ObjectImpl<!>: Derived {}

fun f1(b: Base) = b
