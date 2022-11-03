// FIR_DISABLE_LAZY_RESOLVE_CHECKS
enum class E : Cloneable {
    A;
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun clone(): Any {
        return <!AMBIGUOUS_SUPER!>super<!>.clone()
    }
}
