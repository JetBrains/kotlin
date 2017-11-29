// !WITH_NEW_INFERENCE
enum class E : Cloneable {
    A;
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun clone(): Any {
        return <!NI;TYPE_MISMATCH!><!AMBIGUOUS_SUPER!>super<!>.<!NI;TYPE_MISMATCH!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>clone<!>()<!><!>
    }
}
