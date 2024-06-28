// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

fun <R> materialize(): R = null!!

fun test1(i : Int, x: Any) {
    require(x is String)
    do {
        x.length
    } while (i < 10)
}

//Assignment after smartcast

fun test2(i : Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x = 10
    } while (i < 10)
}

fun test3(i : Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x = ""
    } while (i < 10)
}

fun test4(i : Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x.length
        x += ""
    } while (i < 10)
}

fun test5(i: Int) {
    var x: Any? = materialize()
    require(x is Int)
    do {
        x.inc()
        x++
    } while (i < 10)
}

//Assignment before smartcast

fun test6(i: Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x = 10
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } while (i < 10)
}

fun test7(i: Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x = ""
        x.length
    } while (i < 10)
}

fun test8(i: Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x += ""
        x.length
    } while (i < 10)
}

fun test9(i: Int) {
    var x: Any? = materialize()
    require(x is Int)
    do {
        x++
        x.inc()
    } while (i < 10)
}

//Assignment after loop

fun test10(i: Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x.length
    } while (i < 10)
    x = ""
}

fun test11(i: Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x.length
    } while (i < 10)
    x = 10
}

fun test12(i: Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x.length
    } while (i < 10)
    x += ""
}

fun test13(i: Int) {
    var x: Any? = materialize()
    require(x is Int)
    do {
        x.inc()
    } while (i < 10)
    x++
}

//Assignment before loop

fun test14(i: Int) {
    var x: Any? = materialize()
    require(x is String)
    x = ""
    do {
        x.length
    } while (i < 10)
}

fun test15(i: Int) {
    var x: Any? = materialize()
    require(x is String)
    x = 10
    do {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } while (i < 10)
}

fun test16(i: Int) {
    var x: Any? = materialize()
    require(x is String)
    x += ""
    do {
        x.length
    } while (i < 10)
}

fun test17(i: Int) {
    var x: Any? = materialize()
    require(x is Int)
    x++
    do {
        x.inc()
    } while (i < 10)
}

//With bounds: assignment after smartcast

fun test18(i: Int) {
    val x: Any? = materialize()
    var y = x
    do {
        require(y is String)
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y = ""
    } while (i < 10)
}

fun test19(i: Int) {
    val x: Any? = materialize()
    var y = x
    do {
        require(y is String)
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y = 10
    } while (i < 10)
}

fun test20(i: Int) {
    val x: Any? = materialize()
    var y = x
    do {
        require(y is String)
        x.length
        y += ""
    } while (i < 10)
}

fun test21(i: Int) {
    val x: Any? = materialize()
    var y = x
    do {
        require(y is Int)
        x.inc()
        y++
    } while (i < 10)
}

//With bounds: assignment before smartcast

fun test22(i: Int) {
    val x: Any? = materialize()
    var y = x
    do {
        require(y is String)
        y = ""
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } while (i < 10)
}

fun test23(i: Int) {
    val x: Any? = materialize()
    var y = x
    do {
        require(y is String)
        y = 10
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } while (i < 10)
}

fun test24(i: Int) {
    val x: Any? = materialize()
    var y = x
    do {
        require(y is String)
        y += ""
        x.length
    } while (i < 10)
}

fun test25(i: Int) {
    val x: Any? = materialize()
    var y = x
    do {
        require(y is Int)
        y++
        x.inc()
    } while (i < 10)
}

// Assignment with bounds inside the loop

fun test26(i: Int) {
    val x: Any? = materialize()
    var y = x
    require(y is String)
    do {
        y = 10
        y = x
        x.length
        y.length
    } while (i < 10)
}

fun test27(i: Int) {
    val x: Any? = materialize()
    var y = x
    require(y is String)
    do {
        y = ""
        y = x
        x.length
        y.length
    } while (i < 10)
}

fun test28(i: Int) {
    val x: Any? = materialize()
    var y = x
    require(y is String)
    do {
        y <!UNRESOLVED_REFERENCE!>+=<!> ""
        y = x
        x.length
        y.length
    } while (i < 10)
}

fun test29(i: Int) {
    val x: Any? = materialize()
    var y = x
    require(y is Int)
    do {
        y<!UNRESOLVED_REFERENCE!>++<!>
        y = x
        x.inc()
        y.inc()
    } while (i < 10)
}

//Nested loop

fun test30(i: Int, j: Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        do {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        } while (i < 10)
        x = 10
    }while (j < 10)
}

fun test31(i: Int, j: Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        do {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        } while (i < 10)
        x = ""
    }while (j < 10)
}

fun test32(i: Int, j: Int) {
    var x: Any? = materialize()
    require(x is String)
    do {
        x.length
        do {
            x.length
        } while (i < 10)
        x += ""
    } while (j < 10)
}

fun test33(i: Int, j: Int) {
    var x: Any? = materialize()
    require(x is Int)
    do {
        x.inc()
        do {
            x.inc()
        } while (i < 10)
        x++
    } while(j < 10)
}

// Result is saved in var

fun test34(i: Int) {
    var x: Any? = materialize()
    val state: Boolean = x is String
    do {
        if (state) {
            x.length
        }
    } while (i < 10)
}

fun test35(i: Int) {
    var x: Any? = materialize()
    var state: Boolean = x is String
    do {
        if (state) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
        state = false
    } while (i < 10)
}
