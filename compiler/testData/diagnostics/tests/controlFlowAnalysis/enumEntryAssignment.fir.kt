// ISSUE: KT-68531
enum class Some {
    A {
        init {
            <!VAL_REASSIGNMENT!>A<!> = null!!
            <!UNINITIALIZED_ENUM_ENTRY, VAL_REASSIGNMENT!>B<!> = null!!
        }
    },
    B {
        init {
            <!VAL_REASSIGNMENT!>A<!> = null!!
            <!VAL_REASSIGNMENT!>B<!> = null!!
        }
    };

    init {
        <!UNINITIALIZED_ENUM_ENTRY, VAL_REASSIGNMENT!>A<!> = null!!
        <!UNINITIALIZED_ENUM_ENTRY, VAL_REASSIGNMENT!>B<!> = null!!
    }
}

fun test() {
    Some.<!VAL_REASSIGNMENT!>A<!> = null!!
    Some.<!VAL_REASSIGNMENT!>B<!> = null!!
}
