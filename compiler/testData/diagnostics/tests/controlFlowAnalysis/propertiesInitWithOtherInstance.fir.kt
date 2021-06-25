class A(val next: A? = null) {
    val x: String
    init {
        next?.x = "a"
        <!VAL_REASSIGNMENT!>x<!> = "b"
        <!VAL_REASSIGNMENT!>this.x<!> = "c"
        <!VAL_REASSIGNMENT!>x<!> = "d" // don't repeat the same diagnostic again with this receiver
        <!VAL_REASSIGNMENT!>this.x<!> = "e"

        <!VAL_REASSIGNMENT!>next?.x<!> = "f"
    }
}
