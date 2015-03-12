class A(val next: A? = null) {
    val x: String
    init {
        <!VAL_REASSIGNMENT!>next?.x<!> = "a"
        x = "b"
        <!VAL_REASSIGNMENT!>this.x<!> = "c"
        x = "d" // don't repeat the same diagnostic again with this receiver
        this.x = "e"

        <!VAL_REASSIGNMENT!>next?.x<!> = "f"
    }
}
