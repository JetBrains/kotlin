// !WITH_NEW_INFERENCE
enum class E : Cloneable {
    A;
    override fun clone(): Any {
        return super.<!UNRESOLVED_REFERENCE!>clone<!>()
    }
}
