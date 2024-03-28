// WITH_STDLIB
// DIAGNOSTICS: -ERROR_SUPPRESSION, -DEBUG_INFO_SMARTCAST
import kotlin.contracts.*

@Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE_FUTURE_ERROR")
fun atLeastOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
}

@Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE_FUTURE_ERROR")
fun exactlyOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun runWithoutContract(block: () -> Unit): Unit = block()

fun <R> materialize(): R = null!!

fun test1() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x = ""
        atLeastOnce {
            x.length
        }
    }
}

fun test2() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x = ""
        exactlyOnce {
            x.length
        }
    }
}

fun test3() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x = ""
        runWithoutContract {
            x.length
        }
    }
}

fun test4() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x = materialize()
        atLeastOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test5() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x = materialize()
        exactlyOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test6() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x = materialize()
        runWithoutContract {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test7() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x = 1
        x = ""
        runWithoutContract {
            x.length
        }
    }
}

fun test8() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x = 1
        x = ""
        atLeastOnce {
            x.length
        }
    }
}

fun test9() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x = 1
        x = ""
        exactlyOnce {
            x.length
        }
    }
}

fun test10() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x = 1
        x = ""
        exactlyOnce {
            x.length
        }
    }
}

fun test11() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x = 1
        x = ""
        atLeastOnce {
            x.length
        }
    }
}

fun test12() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x = 1
        x = ""
        runWithoutContract {
            x.length
        }
    }
}

fun test13() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            x = ""
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
}

fun test14() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            x = ""
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
}

fun test15() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            x = ""
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
}

fun test16() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            x = materialize()
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test17() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            x = materialize()
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test18() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            x = materialize()
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test19() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = ""
        }
    }
}

fun test20() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = ""
        }
    }
}

fun test21() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = ""
        }
    }
}

fun test22() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = materialize()
        }
    }
}

fun test23() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = materialize()
        }
    }
}

fun test24() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = materialize()
        }
    }
}

fun test25() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = ""
    }
}

fun test26() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = ""
    }
}

fun test27() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = ""
    }
}

fun test28() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = materialize()
    }
}

fun test29() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = materialize()
    }
}

fun test30() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = materialize()
    }
}

fun test31() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            for (k in 1..2) {
                x.<!UNRESOLVED_REFERENCE!>length<!>
                x = ""
            }
        }
    }
}

fun test32() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            for (k in 1..2) {
                x.<!UNRESOLVED_REFERENCE!>length<!>
                x = ""
            }
        }
    }
}

fun test33() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            for (k in 1..2) {
                x.<!UNRESOLVED_REFERENCE!>length<!>
                x = ""
            }
        }
    }
}

fun test34() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x = ""
        atLeastOnce {
            x.length
        }
    }
}

fun test35() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x = ""
        exactlyOnce {
            x.length
        }
    }
}

fun test36() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x = ""
        runWithoutContract {
            x.length
        }
    }
}

fun test37() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x = materialize()
        atLeastOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test38() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x = materialize()
        exactlyOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test39() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x = materialize()
        runWithoutContract {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test40() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        exactlyOnce {
            x = ""
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
}

fun test41() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        atLeastOnce {
            x = ""
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
}

fun test42() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        runWithoutContract {
            x = ""
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
}

fun test43() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        exactlyOnce {
            x = materialize()
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test44() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        atLeastOnce {
            x = materialize()
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test45() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        runWithoutContract {
            x = materialize()
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test46() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        exactlyOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            x = ""
        }
    }
}

fun test47() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        atLeastOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            x = ""
        }
    }
}

fun test48() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        runWithoutContract {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            x = ""
        }
    }
}

fun test49() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        exactlyOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            x = materialize()
        }
    }
}

fun test50() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        atLeastOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            x = materialize()
        }
    }
}

fun test51() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        runWithoutContract {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            x = materialize()
        }
    }
}

fun test52() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        exactlyOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        x = ""
    }
}

fun test53() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        atLeastOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        x = ""
    }
}

fun test54() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        runWithoutContract {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        x = ""
    }
}

fun test55() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        exactlyOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        x = materialize()
    }
}

fun test56() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        atLeastOnce {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        x = materialize()
    }
}

fun test57() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        runWithoutContract {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        x = materialize()
    }
}