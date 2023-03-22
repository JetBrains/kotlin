// IGNORE_REVERSED_RESOLVE
// ISSUE: KT-55493
// WITH_STDLIB

val Some.z: String
    get() = "ok"

class Some {
    val x: String
    val y: String

    init {
        x = "ok"
        <!VAL_REASSIGNMENT!>x<!> = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"

        fun foo() {
            <!VAL_REASSIGNMENT!>x<!> = "error"
            <!CAPTURED_MEMBER_VAL_INITIALIZATION!>y<!> = "error"
            <!VAL_REASSIGNMENT!>z<!> = "error"
        }
    }

    val a: String = run {
        // these are all on this@run, which is not guaranteed to be this@Some
        <!VAL_REASSIGNMENT!>x<!> = "error"
        <!VAL_REASSIGNMENT!>y<!> = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"
        "hello"
    }

    val b: String = 123.run {
        // now this@run is an Int, so these are on this@Some
        <!VAL_REASSIGNMENT!>x<!> = "error"
        y = "ok"
        <!VAL_REASSIGNMENT!>y<!> = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"
        "there"
    }

    init {
        <!VAL_REASSIGNMENT!>x<!> = "error"
        <!VAL_REASSIGNMENT!>y<!> = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"
    }
}
