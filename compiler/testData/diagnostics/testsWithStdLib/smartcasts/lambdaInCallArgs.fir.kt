fun foo(x: Int, y: Any?, z: Int) {}

fun myRun(block: () -> Unit): Any? {
    return null
}

fun test_1() {
    var x: String? = null
    if (x != null) {
        foo(
            x.length, // stable smartcast
            run { x = "" },
            x.length  // stable smartcast
        )
    }
}

fun test_2() {
    var x: String? = null
    if (x != null) {
        foo(
            x.length, // stable smartcast
            myRun { x = "" },
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length  // unstable smartcast
        )
    }
}

fun test_3() {
    var x: String? = null
    if (x != null) {
        foo(
            x.length, // stable smartcast
            { x = "" },
            x.length  // stable smartcast
        )
    }
}
