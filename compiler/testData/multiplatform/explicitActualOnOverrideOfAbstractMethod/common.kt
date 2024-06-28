expect abstract class Base {
    abstract fun foo()
}

expect class DerivedImplicit : Base {
    override fun foo()
}
