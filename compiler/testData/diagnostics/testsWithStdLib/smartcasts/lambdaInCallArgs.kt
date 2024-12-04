// RUN_PIPELINE_TILL: FRONTEND
fun foo(x: Int, y: Any?, z: Int) {}

fun myRun(block: () -> Unit): Any? {
    return null
}

fun test_1() {
    var x: String? = null
    if (x != null) {
        foo(
            <!DEBUG_INFO_SMARTCAST!>x<!>.length, // stable smartcast
            run { x = "" },
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length  // can be stable smartcast
        )
    }
}

fun test_2() {
    var x: String? = null
    if (x != null) {
        foo(
            <!DEBUG_INFO_SMARTCAST!>x<!>.length, // stable smartcast
            myRun { x = "" },
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length  // unstable smartcast
        )
    }
}

fun test_3() {
    var x: String? = null
    if (x != null) {
        foo(
            <!DEBUG_INFO_SMARTCAST!>x<!>.length, // stable smartcast
            { x = "" },
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length  // stable smartcast
        )
    }
}

fun test_4() {
    var x: String? = null
    if (x != null) {
        foo(
            <!DEBUG_INFO_SMARTCAST!>x<!>.length, // stable smartcast
            run { x = null },
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length  // either unstable or not a smartcast
        )
    }
}
