enum class E : Cloneable {
    A;
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun clone(): Any {
        return <!AMBIGUOUS_SUPER!>super<!>.clone()
    }
}
