interface Base {
    fun foo()
}
val String.test: Base = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD!>object: Base<!> {
    override fun foo() {
        this<!UNRESOLVED_REFERENCE!>@test<!>
    }
}