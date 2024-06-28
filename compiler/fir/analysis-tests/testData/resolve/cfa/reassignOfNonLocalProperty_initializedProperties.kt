// ISSUE: KT-55493, KT-59744
// WITH_STDLIB

val z: String = "ok"

val Some.y: String
    get() = "ok"

class Some {
    val x: String = "ok"

    init {
        <!VAL_REASSIGNMENT!>x<!> = "error"
        <!VAL_REASSIGNMENT!>y<!> = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"
    }

    val a: String = run {
        <!VAL_REASSIGNMENT!>x<!> = "error"
        <!VAL_REASSIGNMENT!>y<!> = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"
        "hello"
    }

    var b: String = "hello"
        get() {
            <!VAL_REASSIGNMENT!>x<!> = "error"
            <!VAL_REASSIGNMENT!>y<!> = "error"
            <!VAL_REASSIGNMENT!>z<!> = "error"
            return field
        }
        set(value) {
            <!VAL_REASSIGNMENT!>x<!> = value
            <!VAL_REASSIGNMENT!>y<!> = value
            <!VAL_REASSIGNMENT!>z<!> = value
            field = value
        }

    var c: String
        get() {
            <!VAL_REASSIGNMENT!>x<!> = "error"
            <!VAL_REASSIGNMENT!>y<!> = "error"
            <!VAL_REASSIGNMENT!>z<!> = "error"
            return "hello"
        }
        set(value) {
            <!VAL_REASSIGNMENT!>x<!> = value
            <!VAL_REASSIGNMENT!>y<!> = value
            <!VAL_REASSIGNMENT!>z<!> = value
        }

    fun test_1() {
        <!VAL_REASSIGNMENT!>x<!> = "error"
        <!VAL_REASSIGNMENT!>y<!> = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"
    }
}

fun Some.test_2() {
    <!VAL_REASSIGNMENT!>x<!> = "error"
    <!VAL_REASSIGNMENT!>y<!> = "error"
    <!VAL_REASSIGNMENT!>z<!> = "error"
}

fun test_3(some: Some) {
    some.<!VAL_REASSIGNMENT!>x<!> = "error"
    some.<!VAL_REASSIGNMENT!>y<!> = "error"
    <!VAL_REASSIGNMENT!>z<!> = "error"
}
