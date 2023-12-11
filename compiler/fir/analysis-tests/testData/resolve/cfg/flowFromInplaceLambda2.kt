// !DUMP_CFG

fun <T> foo(x: T?, i: Int, y: T) {}

fun <T> id(x: T): T = x

fun <T> n(): T? = null

fun someCompletedCall(arg: Int) = arg

fun test1(x: String?) {
    foo(
        id(run { x as String; n() }),
        1,
        run { x<!UNSAFE_CALL!>.<!>length; 123 } // Bad (resolution order undefined)
    )
    x<!UNSAFE_CALL!>.<!>length // Bad: KT-37838 -> OK (x as String unconditional)
}

fun test2(x: String?) {
    foo(
        id(run { x as String; n() }),
        someCompletedCall(1),
        run { x<!UNSAFE_CALL!>.<!>length; 123 } // Bad (resolution order undefined)
    )
    x<!UNSAFE_CALL!>.<!>length // OK (x as String unconditional)
}

fun test3(x: String?) {
    foo(
        id(run { x as String; n() }),
        if (true) 1 else 2,
        run { x<!UNSAFE_CALL!>.<!>length; 123 } // Bad (resolution order undefined)
    )
    x<!UNSAFE_CALL!>.<!>length // Bad: KT-37838 -> OK (x as String unconditional)
}

fun test4(x: String?) {
    var p = x
    if (p != null) {
        foo(
            id(if (true) run { p = null; n() } else run { n() }),
            1,
            run { <!SMARTCAST_IMPOSSIBLE!>p<!>.length; 123 } // Bad (p = null possible)
        )
        p<!UNSAFE_CALL!>.<!>length // Bad (p = null possible)
    }
}

fun test5(x: String?, y: String?) {
    foo(
        y?.let { x as String; n() },
        1,
        run { "" }
    )
    x<!UNSAFE_CALL!>.<!>length // Bad (x as String conditional)
}

fun test6(x: String?) {
    foo(
        id(if (true) run { x as String; n() } else run { x as String; n() }),
        1,
        run { x<!UNSAFE_CALL!>.<!>length; 123 } // Bad (resolution order undefined)
    )
    x<!UNSAFE_CALL!>.<!>length // Bad: KT-37838 -> OK (x as String in both branches)
}

fun test7(x: String?) {
    var p = x
    if (p != null) {
        foo(
            id(run { p = null; n() }),
            1,
            run { <!SMARTCAST_IMPOSSIBLE!>p<!>.length; 123 } // Bad (p = null)
        )
        p<!UNSAFE_CALL!>.<!>length // Bad (p = null)
    }
}
