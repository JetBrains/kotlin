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

//Assigment before lambda
fun test1() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x += ""
        atLeastOnce {
            x.length
        }
    }
}

fun test2() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x += ""
        exactlyOnce {
            x.length
        }
    }
}

fun test3() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        x += ""
        runWithoutContract {
            x.length
        }
    }
}

fun test4() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        x++
        atLeastOnce {
            x.inc()
        }
    }
}

fun test5() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        x++
        exactlyOnce {
            x.inc()
        }
    }
}

fun test6() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        x++
        runWithoutContract {
            x.inc()
        }
    }
}

//Assigment inside lambda before smartcast

fun test7() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            x += ""
            x.length
        }
    }
}

fun test8() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            x += ""
            x.length
        }
    }
}

fun test9() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            x += ""
            x.length
        }
    }
}

fun test10() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        exactlyOnce {
            x++
            x.inc()
        }
    }
}

fun test11() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        atLeastOnce {
            x++
            x.inc()
        }
    }
}

fun test12() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        runWithoutContract {
            x++
            x.inc()
        }
    }
}

//Assigment inside lambda after smartcast

fun test13() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            x.length
            x += ""
        }
    }
}

fun test14() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            x.length
            x += ""
        }
    }
}

fun test15() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            x.length
            x += ""
        }
    }
}

fun test16() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        exactlyOnce {
            x.inc()
            x++
        }
    }
}

fun test17() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        atLeastOnce {
            x.inc()
            x++
        }
    }
}

fun test18() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        runWithoutContract {
            x.inc()
            x++
        }
    }
}

//Assigment after lambda

fun test19() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            x.length
        }
        x += ""
    }
}

fun test20() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            x.length
        }
        x += ""
    }
}

fun test21() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            x.length
        }
        x += ""
    }
}

fun test22() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        exactlyOnce {
            x.inc()
        }
        x++
    }
}

fun test23() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        atLeastOnce {
            x.inc()
        }
        x++
    }
}

fun test24() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        runWithoutContract {
            x.inc()
        }
        x++
    }
}

// Nested cycle

fun test25() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        exactlyOnce {
            for (j in 1..2) {
                x.length
                x += ""
            }
        }
    }
}

fun test26() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        atLeastOnce {
            for (j in 1..2) {
                x.length
                x += ""
            }
        }
    }
}

fun test27() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is String)
        runWithoutContract {
            for (j in 1..2) {
                x.length
                x += ""
            }
        }
    }
}

fun test28() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        exactlyOnce {
            for (j in 1..2) {
                x.inc()
                x++
            }
        }
    }
}

fun test29() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        atLeastOnce {
            for (j in 1..2) {
                x.inc()
                x++
            }
        }
    }
}

fun test30() {
    var x: Any? = materialize()
    for (i in 1..10) {
        require(x is Int)
        runWithoutContract {
            for (j in 1..2) {
                x.inc()
                x++
            }
        }
    }
}

//While with bounds: assignment inside while

fun test31() {
    val x: Any? = materialize()
    var y = x
    exactlyOnce {
        while (y is String) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            y += ""
        }
    }
}

fun test32() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        while (y is String) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            y += ""
        }
    }
}

fun test33() {
    val x: Any? = materialize()
    var y = x
    runWithoutContract {
        while (y is String) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            y += ""
        }
    }
}

fun test34() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        while (y is Int) {
            x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
            y++
        }
    }
}

fun test35() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        while (y is Int) {
            x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
            y++
        }
    }
}

fun test36() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        while (y is Int) {
            x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
            y++
        }
    }
}

//While with bounds: assignment after while

fun test37() {
    val x: Any? = materialize()
    var y = x
    exactlyOnce {
        require(y is String)
        while (true) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            break
        }
        y += ""
    }
}

fun test38() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        require(y is String)
        while (true) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            break
        }
        y += ""
    }
}

fun test39() {
    val x: Any? = materialize()
    var y = x
    runWithoutContract {
        require(y is String)
        while (true) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
            break
        }
        y += ""
    }
}

fun test40() {
    val x: Any? = materialize()
    var y = x
    exactlyOnce {
        require(y is Int)
        while (true) {
            x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
            break
        }
        y++
    }
}

fun test41() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        require(y is Int)
        while (true) {
            x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
            break
        }
        y++
    }
}

fun test42() {
    val x: Any? = materialize()
    var y = x
    runWithoutContract {
        require(y is Int)
        while (true) {
            x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
            break
        }
        y++
    }
}
