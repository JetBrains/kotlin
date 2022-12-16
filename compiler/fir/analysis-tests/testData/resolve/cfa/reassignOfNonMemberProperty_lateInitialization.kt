// ISSUE: KT-55493
// WITH_STDLIB

val Some.z: String
    get() = "ok"

class Some {
    val x: String
    val y: String

    init {
        x = "ok"
        x = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"
    }

    val a: String = run {
        x = "error"
        y = "ok"
        y = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"
        "hello"
    }

    init {
        x = "error"
        y = "error"
        <!VAL_REASSIGNMENT!>z<!> = "error"
    }
}

