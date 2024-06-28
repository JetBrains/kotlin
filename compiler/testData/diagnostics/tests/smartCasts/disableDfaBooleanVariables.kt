// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// LANGUAGE:-DfaBooleanVariables

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun test1() {
    val nullableString: String? = ""
    val test = nullableString != null
    if (test) nullableString<!UNSAFE_CALL!>.<!>length
    if (nullableString != null) nullableString.length
}

class A {
    val a: String? = ""
}
fun test3(a: A) {
    val test = a.a != null
    if (test) a.a<!UNSAFE_CALL!>.<!>length
    if (a.a != null) {
        a.a.length
    }
}

fun test4() {
    val nullableAny: Any? = ""
    val test = nullableAny != null && nullableAny is String?
    if (test) nullableAny.<!UNRESOLVED_REFERENCE!>length<!>
    if (nullableAny != null && nullableAny is String?) nullableAny.length
}

fun test7(bar: Any) {
    val test = bar is String
    if (test) bar.<!UNRESOLVED_REFERENCE!>length<!>
    if (bar is String) bar.length
}

fun test8(bar: Any) {
    val test = bar is String
    when {
        test -> bar.<!UNRESOLVED_REFERENCE!>length<!>
    }
    when {
        bar is String -> bar.length
    }
}

fun test9(bar: Any) {
    bar is String && bar.length == 0
    val test = bar is String
    test && bar.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test10(bar: Any) {
    val test = bar !is String
    run {
        if (test) return@run
        bar.<!UNRESOLVED_REFERENCE!>length<!>
    }
    run {
        if (bar !is String) return@run
        bar.length
    }
}

fun test11(bar: Any, baz: Any) {
    val test = baz is String && baz === bar
    val test2 = baz is String
    val test3 = baz === bar
    val test4 = test2 && test3

    if (test) bar.<!UNRESOLVED_REFERENCE!>length<!>
    if (test2 && test3) bar.<!UNRESOLVED_REFERENCE!>length<!>
    if (baz is String && test3) bar.<!UNRESOLVED_REFERENCE!>length<!>
    if (test2 && baz === bar) bar.<!UNRESOLVED_REFERENCE!>length<!>
    if (test4) bar.<!UNRESOLVED_REFERENCE!>length<!>

    if (baz is String && baz === bar) bar.length
}

fun test12(bar: Any) {
    val test = bar !is String
    if (!test) bar.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test13(bar: Any) {
    val test = bar is String
    if (test == true) bar.<!UNRESOLVED_REFERENCE!>length<!>
    when (test) {
        true -> bar.<!UNRESOLVED_REFERENCE!>length<!>
        false -> {}
    }

    val test2: Boolean? = bar is String
    if (test2 == true) bar.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test14(bar: Any) {
    val test = (bar as? String) != null
    if (test) bar.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test15(bar: Any) {
    val test = bar is String
    val test2 = test
    if (test2) bar.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test16(bar: Any) {
    val x = bar.isString()
    if (x) bar.<!UNRESOLVED_REFERENCE!>length<!>
    if (bar.isString()) bar.length
}

@OptIn(ExperimentalContracts::class)
fun Any.isString(): Boolean {
    contract {
        returns(true) implies (this@isString is String)
    }
    return this is String
}

fun String.isEmpty(): Boolean = length == 0
fun test17(a: String?) {
    val x = a?.length
    if (x != null) a.length

    val y = a?.isEmpty()
    if (y == true) a.length
}
