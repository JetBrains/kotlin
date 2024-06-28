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

fun test1() {
    var x: Any? = materialize()
    require(x is String)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        x = 10
    }
}

fun test2() {
    var x: Any? = materialize()
    require(x is String)
    exactlyOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        x = 10
    }
}

fun test3() {
    var x: Any? = materialize()
    require(x is String)
    atLeastOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        x = 10
    }
}

fun test4() {
    var x: Any? = materialize()
    require(x is String)
    runWithoutContract {
        x = ""
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun test5() {
    var x: Any? = materialize()
    require(x is String)
    exactlyOnce {
        x = ""
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun test6() {
    var x: Any? = materialize()
    require(x is String)
    atLeastOnce {
        x = ""
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun test7() {
    var x: Any? = materialize()
    require(x is String)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        runWithoutContract {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = 10
    }
}

fun test8() {
    var x: Any? = materialize()
    require(x is String)
    exactlyOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        exactlyOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = 10
    }
}

fun test9() {
    var x: Any? = materialize()
    require(x is String)
    atLeastOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        atLeastOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = 10
    }
}

fun test10() {
    var x: Any? = materialize()
    runWithoutContract {
        val b = x is String
        if (b) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test11(x: Any) {
    exactlyOnce {
        val b = x is String
        if (b) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test12(x: Any) {
    atLeastOnce {
        val b = x is String
        if (b) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test13(x: Any) {
    if (x is String) {
        runWithoutContract {
            x.length
        }
    }
}

fun test14(x: Any) {
    if (x is String) {
        exactlyOnce {
            x.length
        }
    }
}

fun test15(x: Any) {
    if (x is String) {
        atLeastOnce {
            x.length
        }
    }
}

fun test16(x: Any) {
    atLeastOnce {
        when (x) {
            is String -> x.length
        }
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test17(x: Any) {
    runWithoutContract {
        when (x) {
            is String -> x.length
        }
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test18(x: Any) {
    exactlyOnce {
        when (x) {
            is String -> x.length
        }
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test19(x: Any) {
    when (x) {
        is String -> atLeastOnce {
            x.length
        }
    }
}

fun test20(x: Any) {
    when (x) {
        is String -> runWithoutContract {
            x.length
        }
    }
}

fun test21(x: Any) {
    when (x) {
        is String -> exactlyOnce {
            x.length
        }
    }
}

fun test22(x: Any) {
    val state: Boolean = x is String
    runWithoutContract {
        if (state) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test23(x: Any) {
    val state: Boolean = x is String
    exactlyOnce {
        if (state) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test24(x: Any) {
    val state: Boolean = x is String
    atLeastOnce {
        if (state) {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

fun test25() {
    var x: Any? = materialize()
    require(x is String)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    x = ""
}

fun test26() {
    var x: Any? = materialize()
    require(x is String)
    exactlyOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    x = ""
}

fun test27() {
    var x: Any? = materialize()
    require(x is String)
    atLeastOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    x = ""
}

fun test28(x: Any) {
    runWithoutContract {
        require(x is String)
        x.length
    }
    runWithoutContract {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test29(x: Any) {
    exactlyOnce {
        require(x is String)
        x.length
    }
    exactlyOnce {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test30(x: Any) {
    atLeastOnce {
        require(x is String)
        x.length
    }
    atLeastOnce {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test31(x: Any) {
    runWithoutContract {
        require(x is String)
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test32(x: Any) {
    exactlyOnce {
        require(x is String)
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test33(x: Any) {
    atLeastOnce {
        require(x is String)
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test34() {
    var x: Any? = materialize()
    runWithoutContract {
        require(x is String)
        x = 10
    }
    runWithoutContract {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test35() {
    var x: Any? = materialize()
    exactlyOnce {
        require(x is String)
        x = 10
    }
    exactlyOnce {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test36() {
    var x: Any? = materialize()
    atLeastOnce {
        require(x is String)
        x = 10
    }
    atLeastOnce {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test37() {
    var x: Any? = materialize()
    require(x is String)
    fun local() {
        atLeastOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = ""
    }
}

fun test38() {
    var x: Any? = materialize()
    require(x is String)
    fun local() {
        exactlyOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = ""
    }
}

fun test39() {
    var x: Any? = materialize()
    require(x is String)
    fun local() {
        runWithoutContract {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
        x = ""
    }
}

fun test40() {
    var x: Any? = materialize()
    require(x is String)
    fun local() {
        atLeastOnce {
            x.length
        }
    }
}

fun test41() {
    var x: Any? = materialize()
    require(x is String)
    fun local() {
        exactlyOnce {
            x.length
        }
    }
}

fun test42() {
    var x: Any? = materialize()
    require(x is String)
    fun local() {
        runWithoutContract {
            x.length
        }
    }
}

fun test43() {
    runWithoutContract {
        var x: Any? = materialize()
        require(x is String)
        x.length
        x = 10
    }
}

fun test44() {
    atLeastOnce {
        var x: Any? = materialize()
        require(x is String)
        x.length
        x = 10
    }
}

fun test45() {
    exactlyOnce {
        var x: Any? = materialize()
        require(x is String)
        x.length
        x = 10
    }
}

fun test46() {
    var x: Any = materialize()
    exactlyOnce {
        while (x is String) {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test47() {
    var x: Any = materialize()
    atLeastOnce {
        while (x is String) {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test48() {
    var x: Any = materialize()
    runWithoutContract {
        while (x is String) {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test49() {
    var x: Any = materialize()
    while (x is String) {
        exactlyOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test50() {
    var x: Any = materialize()
    while (x is String) {
        atLeastOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test51() {
    var x: Any = materialize()
    while (x is String) {
        runWithoutContract {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test52() {
    var x: Any = materialize()
    exactlyOnce {
        do {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        } while (x is String)
    }
}

fun test53() {
    var x: Any = materialize()
    atLeastOnce {
        do {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        } while (x is String)
    }
}

fun test54() {
    var x: Any = materialize()
    runWithoutContract {
        do {
            x.<!UNRESOLVED_REFERENCE!>length<!>
        } while (x is String)
    }
}

fun test55() {
    var x: Any = materialize()
    runWithoutContract {
        for (i in 1..3) {
            require(x is String)
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test56() {
    var x: Any = materialize()
    atLeastOnce {
        for (i in 1..3) {
            require(x is String)
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test57() {
    var x: Any = materialize()
    exactlyOnce {
        for (i in 1..3) {
            require(x is String)
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test58() {
    var x: Any = materialize()
    for (i in 1..3) {
        require(x is String)
        runWithoutContract {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test59() {
    var x: Any = materialize()
    for (i in 1..3) {
        require(x is String)
        atLeastOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test60() {
    var x: Any = materialize()
    for (i in 1..3) {
        require(x is String)
        exactlyOnce {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            x = 10
        }
    }
}

fun test61() {
    var x: String? = materialize()
    runWithoutContract {
        x?.length ?: -1
        x = null
    }
}

fun test62() {
    var x: String? = materialize()
    atLeastOnce {
        x?.length ?: -1
        x = null
    }
}

fun test63() {
    var x: String? = materialize()
    exactlyOnce {
        x?.length ?: -1
        x = null
    }
}

fun test64() {
    var x: Any = materialize()
    require(x is String)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    for (i in 1..3) {
        x = 10
    }
}

fun test65() {
    var x: Any = materialize()
    require(x is String)
    atLeastOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    for (i in 1..3) {
        x = 10
    }
}

fun test66() {
    var x: Any = materialize()
    require(x is String)
    exactlyOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    for (i in 1..3) {
        x = 10
    }
}
