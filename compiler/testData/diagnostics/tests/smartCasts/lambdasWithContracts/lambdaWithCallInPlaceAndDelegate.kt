// ISSUE: KT-28806
// FIR_IDENTICAL
// WITH_STDLIB
// DIAGNOSTICS: -ERROR_SUPPRESSION
import kotlin.contracts.*
import kotlin.reflect.KProperty

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

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Any {
        TODO()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Any) {
        TODO()
    }
}

fun test1() {
    var x: Any by Delegate()
    require(x is String)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun test2() {
    var x: Any by Delegate()
    require(x is String)
    atLeastOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun test3() {
    var x: Any by Delegate()
    require(x is String)
    exactlyOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun test4() {
    val x: Any by Delegate()
    require(x is String)
    runWithoutContract {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun test5() {
    val x: Any by Delegate()
    require(x is String)
    atLeastOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun test6() {
    val x: Any by Delegate()
    require(x is String)
    exactlyOnce {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}
