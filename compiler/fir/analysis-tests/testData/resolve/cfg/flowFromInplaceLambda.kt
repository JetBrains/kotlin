// !DUMP_CFG

fun takeInt(x: Int) {}

fun <K> select(vararg x: K): K = x[0]
fun <T> id(x: T): T = x
fun <K> materialize(): K = null!!

fun <R> myRun(block: () -> R): R = block()

fun test_1(x: Any) {
    run {
        x as Int
    }
    takeInt(x) // OK
}

fun test_2(x: Any, y: Any) {
    val a = select(
        id(
            run {
                y.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad
                x as Int
            }
        ),
        y as Int,
        run {
            x.inc() // Should be "Bad" but "OK" is fine
            y.inc() // OK
            1
        }
    )
    takeInt(x) // OK
    takeInt(y) // OK
    takeInt(a) // OK
}

fun test_3(x: Any, y: Any) {
    val a = select(
        id(
            run {
                y.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad
                x as Int
                materialize()
            }
        ),
        run {
            y as Int
            x.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad
            y.inc() // OK
            1
        }
    )
    takeInt(x) // OK
    takeInt(y) // OK
    takeInt(a) // OK
}

fun test_4(x: Any, y: Any) {
    val a = select(
        id(
            myRun {
                y.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad
                x as Int
            }
        ),
        y as Int,
        myRun {
            x.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad
            y.inc() // OK
            1
        }

    )
    <!INAPPLICABLE_CANDIDATE!>takeInt<!>(x) // Bad
    takeInt(y) // OK
    takeInt(a) // Bad
}

fun test_5() {
    val x: Int = select(run { materialize() }, run { materialize() })
    takeInt(x)
}

fun test_6() {
    val x: String = id(
        myRun {
            run {
                materialize()
            }
        }
    )
}