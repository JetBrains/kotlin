enum class E : Cloneable {
    A;
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun clone(): Any {
        return <!AMBIGUOUS_SUPER!>super<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>clone<!>()
    }
}
