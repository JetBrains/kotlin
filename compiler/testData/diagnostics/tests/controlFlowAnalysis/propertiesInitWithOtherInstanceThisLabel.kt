class A(val next: A? = null) {
    val x: String
    init {
        <!VAL_REASSIGNMENT!>next?.x<!> = "a"
        this@A.x = "b"
        <!VAL_REASSIGNMENT!>this.x<!> = "c"
        x = "d" // don't repeat the same diagnostic again with this receiver
        this@A.x = "e"

        <!VAL_REASSIGNMENT!>next?.x<!> = "f"
    }
}
