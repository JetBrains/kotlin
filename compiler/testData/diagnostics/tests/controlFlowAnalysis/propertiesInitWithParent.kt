// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74421

open class Base {
    val x: String = "ok"
}

class InitViaSuper : Base() {
    init {
        <!VAL_REASSIGNMENT!>super.x<!> = "error"
    }
}

class InitViaThis : Base() {
    init {
        <!VAL_REASSIGNMENT!>this.x<!> = "error"
    }
}


class InitViaImplicit : Base() {
    init {
        <!VAL_REASSIGNMENT!>x<!> = "error"
    }
}
