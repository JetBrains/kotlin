abstract class Bar {
    abstract var bar : String
    fun foo() = "foo" + this.<!NO_BACKING_FIELD_ABSTRACT_PROPERTY!>$bar<!>
}