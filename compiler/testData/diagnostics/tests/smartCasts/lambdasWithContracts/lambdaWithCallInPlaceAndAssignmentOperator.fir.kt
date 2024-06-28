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

//Assignment after smartcast

fun test1() {
    var x: Any? = materialize()
    require(x is String)
    atLeastOnce {
        x.length
        x += ""
    }
}

fun test2() {
    var x: Any? = materialize()
    require(x is String)
    exactlyOnce {
        x.length
        x += ""
    }
}

fun test3() {
    var x: Any? = materialize()
    require(x is String)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        <!SMARTCAST_IMPOSSIBLE!>x<!> += ""
    }
}

fun test4() {
    var x: Any? = materialize()
    require(x is Int)
    atLeastOnce {
        x.inc()
        x *= 1
    }
}

fun test5() {
    var x: Any? = materialize()
    require(x is Int)
    exactlyOnce {
        x.inc()
        x *= 1
    }
}

fun test6() {
    var x: Any? = materialize()
    require(x is Int)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.inc()
        <!SMARTCAST_IMPOSSIBLE!>x<!> *= 1
    }
}

fun test7() {
    var x: Any? = materialize()
    require(x is Int)
    atLeastOnce {
        x.inc()
        x %= 1
    }
}

fun test8() {
    var x: Any? = materialize()
    require(x is Int)
    exactlyOnce {
        x.inc()
        x %= 1
    }
}

fun test9() {
    var x: Any? = materialize()
    require(x is Int)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.inc()
        <!SMARTCAST_IMPOSSIBLE!>x<!> %= 1
    }
}

fun test10() {
    var x: Any? = materialize()
    require(x is Int)
    atLeastOnce {
        x.inc()
        x++
    }
}

fun test11() {
    var x: Any? = materialize()
    require(x is Int)
    exactlyOnce {
        x.inc()
        x++
    }
}

fun test12() {
    var x: Any? = materialize()
    require(x is Int)
    runWithoutContract {
        x.inc()
        x++
    }
}

fun test13() {
    var x: Any? = materialize()
    require(x is Int)
    atLeastOnce {
        x.inc()
        --x
    }
}

fun test14() {
    var x: Any? = materialize()
    require(x is Int)
    exactlyOnce {
        x.inc()
        --x
    }
}

fun test15() {
    var x: Any? = materialize()
    require(x is Int)
    runWithoutContract {
        x.inc()
        --x
    }
}

//Assignment before smartcast

fun test16() {
    var x: Any? = materialize()
    require(x is String)
    atLeastOnce {
        x += ""
        x.length
    }
}

fun test17() {
    var x: Any? = materialize()
    require(x is String)
    exactlyOnce {
        x += ""
        x.length
    }
}

fun test18() {
    var x: Any? = materialize()
    require(x is String)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!> += ""
        x.length
    }
}

fun test19() {
    var x: Any? = materialize()
    require(x is Int)
    atLeastOnce {
        x++
        x.inc()
    }
}

fun test20() {
    var x: Any? = materialize()
    require(x is Int)
    exactlyOnce {
        x++
        x.inc()
    }
}

fun test21() {
    var x: Any? = materialize()
    require(x is Int)
    runWithoutContract {
        x++
        x.inc()
    }
}

//Assignment after lambda

fun test22() {
    var x: Any? = materialize()
    require(x is String)
    atLeastOnce {
        x.length
    }
    x += ""
}

fun test23() {
    var x: Any? = materialize()
    require(x is String)
    exactlyOnce {
        x.length
    }
    x += ""
}

fun test24() {
    var x: Any? = materialize()
    require(x is String)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    x += ""
}

fun test25() {
    var x: Any? = materialize()
    require(x is Int)
    atLeastOnce {
        x.inc()
    }
    x++
}

fun test26() {
    var x: Any? = materialize()
    require(x is Int)
    exactlyOnce {
        x.inc()
    }
    x++
}

fun test27() {
    var x: Any? = materialize()
    require(x is Int)
    runWithoutContract {
        x.inc()
    }
    x++
}

//Assignment  before lambda

fun test28() {
    var x: Any? = materialize()
    require(x is String)
    x += ""
    atLeastOnce {
        x.length
    }
}

fun test29() {
    var x: Any? = materialize()
    require(x is String)
    x += ""
    exactlyOnce {
        x.length
    }
}

fun test30() {
    var x: Any? = materialize()
    require(x is String)
    x += ""
    runWithoutContract {
        x.length
    }
}

fun test31() {
    var x: Any? = materialize()
    require(x is Int)
    x++
    atLeastOnce {
        x.inc()
    }
}

fun test32() {
    var x: Any? = materialize()
    require(x is Int)
    x++
    exactlyOnce {
        x.inc()
    }
}

fun test33() {
    var x: Any? = materialize()
    require(x is Int)
    x++
    runWithoutContract {
        x.inc()
    }
}

//With bounds: assignment after smartcast

fun test34() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        require(y is String)
        x.length
        y += ""
    }
}

fun test35() {
    val x: Any? = materialize()
    var y = x
    exactlyOnce {
        require(y is String)
        x.length
        y += ""
    }
}

fun test36() {
    val x: Any? = materialize()
    var y = x
    runWithoutContract {
        require(y is String)
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y <!UNRESOLVED_REFERENCE!>+=<!> ""
    }
}

fun test37() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        require(y is Int)
        x.inc()
        y++
    }
}

fun test38() {
    val x: Any? = materialize()
    var y = x
    exactlyOnce {
        require(y is Int)
        x.inc()
        y++
    }
}

fun test39() {
    val x: Any? = materialize()
    var y = x
    runWithoutContract {
        require(y is Int)
        x.inc()
        y++
    }
}

//With bounds: assignment before smartcast

fun test40() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        require(y is String)
        y += ""
        x.length
    }
}

fun test41() {
    val x: Any? = materialize()
    var y = x
    exactlyOnce {
        require(y is String)
        y += ""
        x.length
    }
}

fun test42() {
    val x: Any? = materialize()
    var y = x
    runWithoutContract {
        require(y is String)
        y <!UNRESOLVED_REFERENCE!>+=<!> ""
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test43() {
    val x: Any? = materialize()
    var y = x
    atLeastOnce {
        require(y is Int)
        y++
        x.inc()
    }
}

fun test44() {
    val x: Any? = materialize()
    var y = x
    exactlyOnce {
        require(y is Int)
        y++
        x.inc()
    }
}

fun test45() {
    val x: Any? = materialize()
    var y = x
    runWithoutContract {
        require(y is Int)
        y++
        x.inc()
    }
}
