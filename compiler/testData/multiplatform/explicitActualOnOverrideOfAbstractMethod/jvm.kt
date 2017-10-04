actual abstract class Base {
    actual abstract fun foo()
}

actual class DerivedImplicit : Base() {
    override fun foo() {}
}