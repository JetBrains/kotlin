// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74421

open class Base {
    val x: String = "ok"
}

class InitViaSuper : Base() {
    init {
        super.<!VAL_REASSIGNMENT!>x<!> = "error"
    }
}

class InitViaThis : Base() {
    init {
        this.<!VAL_REASSIGNMENT!>x<!> = "error"
    }
}


class InitViaImplicit : Base() {
    init {
        <!VAL_REASSIGNMENT!>x<!> = "error"
    }
}
