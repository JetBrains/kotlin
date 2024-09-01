// ISSUE: KT-68531
enum class Some {
    A {
        init {
            <!VAL_REASSIGNMENT, VAL_REASSIGNMENT!>A<!> = null!!
            <!VAL_REASSIGNMENT, VAL_REASSIGNMENT!>B<!> = null!!
        }
    },
    B {
        init {
            <!VAL_REASSIGNMENT!>A<!> = null!!
            <!VAL_REASSIGNMENT!>B<!> = null!!
        }
    };

    init {
        <!INITIALIZATION_BEFORE_DECLARATION!>A<!> = null!!
        <!INITIALIZATION_BEFORE_DECLARATION!>B<!> = null!!
    }
}

fun test() {
    <!VAL_REASSIGNMENT!>Some.A<!> = null!!
    <!VAL_REASSIGNMENT!>Some.B<!> = null!!
}
