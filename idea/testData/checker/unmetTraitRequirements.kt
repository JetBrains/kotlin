open class Base {
}

trait Derived: <warning>Base</warning> {
    fun foo() {
        f1(this@Derived)
    }
}

<error>class DerivedImpl</error>(): Derived {}
<error>object ObjectImpl</error>: Derived {}

fun f1(b: Base) = b

// KT-3006