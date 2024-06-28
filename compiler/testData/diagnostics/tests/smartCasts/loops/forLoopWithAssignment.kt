// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

fun <R> materialize(): R = null!!

fun test1() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x.length
    }
}

//Assignment after smartcast

fun test2() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x = 10
    }
}

fun test3() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x = ""
    }
}

fun test4() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x.length
        x += ""
    }
}

fun test5() {
    var x: Any? = materialize()
    require(x is Int)
    for(i in 1..10) {
        x.inc()
        x++
    }
}

//Assignment before smartcast

fun test6() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x = 10
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test7() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x = ""
        x.length
    }
}

fun test8() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x += ""
        x.length
    }
}

fun test9() {
    var x: Any? = materialize()
    require(x is Int)
    for(i in 1..10) {
        x++
        x.inc()
    }
}

//Assignment after loop

fun test10() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x.length
    }
    x = ""
}

fun test11() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x.length
    }
    x = 10
}

fun test12() {
    var x: Any? = materialize()
    require(x is String)
    for(i in 1..10) {
        x.length
    }
    x += ""
}

fun test13() {
    var x: Any? = materialize()
    require(x is Int)
    for(i in 1..10) {
        x.inc()
    }
    x++
}

//Assignment before loop

fun test14() {
    var x: Any? = materialize()
    require(x is String)
    x = ""
    for(i in 1..10) {
        x.length
    }
}

fun test15() {
    var x: Any? = materialize()
    require(x is String)
    x = 10
    for(i in 1..10) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test16() {
    var x: Any? = materialize()
    require(x is String)
    x += ""
    for(i in 1..10) {
        x.length
    }
}

fun test17() {
    var x: Any? = materialize()
    require(x is Int)
    x++
    for(i in 1..10) {
        x.inc()
    }
}

//With bounds: assignment after smartcast

fun test18() {
    val x: Any? = materialize()
    var y = x
    for (i in 1..10) {
        require(y is String)
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y = ""
    }
}

fun test19() {
    val x: Any? = materialize()
    var y = x
    for (i in 1..10) {
        require(y is String)
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y = 10
    }
}

fun test20() {
    val x: Any? = materialize()
    var y = x
    for (i in 1..10) {
        require(y is String)
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y += ""
    }
}

fun test21() {
    val x: Any? = materialize()
    var y = x
    for (i in 1..10) {
        require(y is Int)
        x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
        y++
    }
}

//With bounds: assignment before smartcast

fun test22() {
    val x: Any? = materialize()
    var y = x
    for (i in 1..10) {
        require(y is String)
        y = ""
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test23() {
    val x: Any? = materialize()
    var y = x
    for (i in 1..10) {
        require(y is String)
        y = 10
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test24() {
    val x: Any? = materialize()
    var y = x
    for (i in 1..10) {
        require(y is String)
        y += ""
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test25() {
    val x: Any? = materialize()
    var y = x
    for (i in 1..10) {
        require(y is Int)
        y++
        x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    }
}

// Assignment with bounds inside the loop

fun test26() {
    val x: Any? = materialize()
    var y = x
    require(y is String)
    for(i in 1..10) {
        y = 10
        y = x
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test27() {
    val x: Any? = materialize()
    var y = x
    require(y is String)
    for(i in 1..10) {
        y = ""
        y = x
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test28() {
    val x: Any? = materialize()
    var y = x
    require(y is String)
    for(i in 1..10) {
        y <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+=<!> ""
        y = x
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test29() {
    val x: Any? = materialize()
    var y = x
    require(y is Int)
    for(i in 1..10) {
        y<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>++<!>
        y = x
        x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
        y.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    }
}

//Nested loop

fun test30() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        for (j in 1..10) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        x = 10
    }
}

fun test31() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        for (j in 1..10) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        x = ""
    }
}

fun test32() {
    var x: Any? = materialize()
    require(x is String)
    for (i in 1..10) {
        x.length
        for (j in 1..10) {
            x.length
        }
        x += ""
    }
}

fun test33() {
    var x: Any? = materialize()
    require(x is Int)
    for (i in 1..10) {
        x.inc()
        for (j in 1..10) {
            x.inc()
        }
        x++
    }
}

// Result is saved in var

fun test34() {
    var x: Any? = materialize()
    val state: Boolean = x is String
    for(i in 1..10) {
        if (state) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test35() {
    var x: Any? = materialize()
    var state: Boolean = x is String
    for(i in 1..10) {
        if (state) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        state = false
    }
}