// !WITH_NEW_INFERENCE
enum class E : Cloneable {
    A;
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun clone(): Any {
        return super.<!UNRESOLVED_REFERENCE!>clone<!>()
    }
}
