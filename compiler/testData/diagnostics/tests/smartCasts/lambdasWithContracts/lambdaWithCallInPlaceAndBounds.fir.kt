// ISSUE: KT-28806
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

fun test1(x: Any) {
    var y = x
    require(y is String)
    runWithoutContract {
        y = 10
        y = x
        x.length
        <!SMARTCAST_IMPOSSIBLE!>y<!>.length
    }
}

fun test2(x: Any) {
    var y = x
    require(y is String)
    exactlyOnce {
        y = 10
        y = x
        x.length
        y.length
    }
}

fun test3(x: Any) {
    var y = x
    require(y is String)
    atLeastOnce {
        y = 10
        y = x
        x.length
        y.length
    }
}

fun test4(x: Any) {
    var y: Any
    require(x is String)
    runWithoutContract {
        y = 10
        y = x
        x.length
        <!SMARTCAST_IMPOSSIBLE!>y<!>.length
    }
}

fun test5(x: Any) {
    var y: Any
    require(x is String)
    exactlyOnce {
        y = 10
        y = x
        x.length
        y.length
    }
}

fun test6(x: Any) {
    var y: Any
    require(x is String)
    atLeastOnce {
        y = 10
        y = x
        x.length
        y.length
    }
}

fun test7(x: Any) {
    var y: Any?
    val b = x is String
    if (b) {
        y = x
        exactlyOnce {
            x.length
            y.length
        }
    }
}

fun test8(x: Any) {
    var y: Any?
    val b = x is String
    if (b) {
        y = x
        atLeastOnce {
            x.length
            y.length
        }
    }
}

fun test9(x: Any) {
    var y: Any?
    val b = x is String
    if (b) {
        y = x
        runWithoutContract {
            x.length
            y.length
        }
    }
}

fun test10(x: Any) {
    var y = x
    runWithoutContract {
        require(y is String)
        x.length
        y.length
    }
}

fun test11(x: Any) {
    var y = x
    atLeastOnce {
        require(y is String)
        x.length
        y.length
    }
}

fun test12(x: Any) {
    var y = x
    exactlyOnce {
        require(y is String)
        x.length
        y.length
    }
}

fun test13() {
    var x: Any? = materialize()
    var y = x
    atLeastOnce {
        require(y is String)
        x = 10
        y = x
        y.<!UNRESOLVED_REFERENCE!>length<!>
        y.inc()
    }
}

fun test14() {
    var x: Any? = materialize()
    var y = x
    exactlyOnce {
        require(y is String)
        x = 10
        y = x
        y.<!UNRESOLVED_REFERENCE!>length<!>
        y.inc()
    }
}

fun test15() {
    var x: Any? = materialize()
    var y = x
    runWithoutContract {
        require(y is String)
        x = 10
        y = x
        y.<!UNRESOLVED_REFERENCE!>length<!>
        y.inc()
    }
}

fun test16(x: Any) {
    var y: Any
    runWithoutContract {
        require(x is String)
        y = x
        y.length
    }
}

fun test17(x: Any) {
    var y: Any
    exactlyOnce {
        require(x is String)
        y = x
        y.length
    }
}

fun test18(x: Any) {
    var y: Any
    atLeastOnce {
        require(x is String)
        y = x
        y.length
    }
}

fun test19(x: Any?) {
    var y: Any
    atLeastOnce {
        if (x != null) {
            y = x
        } else {
            y = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
        }
        require(x is String)
        y.length
    }
}

fun test20(x: Any?) {
    var y: Any
    exactlyOnce {
        if (x != null) {
            y = x
        } else {
            y = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
        }
        require(x is String)
        y.length
    }
}

fun test21(x: Any?) {
    var y: Any
    runWithoutContract {
        if (x != null) {
            y = x
        } else {
            y = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
        }
        require(x is String)
        <!SMARTCAST_IMPOSSIBLE!>y<!>.length
    }
}

fun test22(x: Any) {
    var y: Any = materialize()
    runWithoutContract {
        require(x is String)
        y = x
    }
    y.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test23(x: Any) {
    var y: Any
    exactlyOnce {
        require(x is String)
        y = x
    }
    y.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test24(x: Any) {
    var y: Any
    atLeastOnce {
        require(x is String)
        y = x
    }
    y.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test25(x: Any) {
    var y = x
    exactlyOnce {
        while (x is String) {
            y.length
        }
    }
}

fun test26(x: Any) {
    var y = x
    atLeastOnce {
        while (x is String) {
            y.length
        }
    }
}

fun test27(x: Any) {
    var y = x
    runWithoutContract {
        while (x is String) {
            y.length
        }
    }
}

fun test28() {
    var x: Any? = materialize()
    var y = x
    exactlyOnce {
        while (x is String) {
            y.<!UNRESOLVED_REFERENCE!>length<!>
            x = 10
        }
    }
}

fun test29() {
    var x: Any? = materialize()
    var y = x
    atLeastOnce {
        while (x is String) {
            y.<!UNRESOLVED_REFERENCE!>length<!>
            x = 10
        }
    }
}

fun test30() {
    var x: Any? = materialize()
    var y = x
    runWithoutContract {
        while (x is String) {
            y.<!UNRESOLVED_REFERENCE!>length<!>
            x = 10
        }
    }
}

fun test31() {
    var x: Any? = materialize()
    var y = x
    exactlyOnce {
        while (x is String) {
            y.length
            x.length
        }
        x = 10
    }
}

fun test32() {
    var x: Any? = materialize()
    var y = x
    atLeastOnce {
        while (x is String) {
            y.<!UNRESOLVED_REFERENCE!>length<!>
            x.length
        }
        x = 10
    }
}

fun test33() {
    var x: Any? = materialize()
    var y = x
    runWithoutContract {
        while (x is String) {
            y.<!UNRESOLVED_REFERENCE!>length<!>
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = 10
    }
}