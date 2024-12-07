// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-37115
// LANGUAGE: -InferMoreImplicationsFromBooleanExpressions

fun test1(foo: String?) {
    foo != null || throw IllegalArgumentException()
    foo<!UNSAFE_CALL!>.<!>length
}

fun test2(foo: String?) {
    foo != null || return
    foo<!UNSAFE_CALL!>.<!>length
}

fun test3(foo: String?) {
    while (true) {
        foo != null || continue
        foo<!UNSAFE_CALL!>.<!>length
    }
}

fun test4(foo: String?) {
    while (true) {
        foo == null || break
        foo<!UNSAFE_CALL!>.<!>length
    }
    foo<!UNSAFE_CALL!>.<!>length
}

fun test5(foo: String?) {
    while (true) {
        foo != null || break
        foo<!UNSAFE_CALL!>.<!>length
    }
    foo<!UNSAFE_CALL!>.<!>length
}

fun test6(foo: String?) {
    var i = 0
    while (i == 0) {
        foo == null || break
        foo<!UNSAFE_CALL!>.<!>length
    }
    foo<!UNSAFE_CALL!>.<!>length
}

fun test7(foo: String?) {
    for (i in 1..3) {
        foo != null || break
        foo<!UNSAFE_CALL!>.<!>length
    }
}

fun test8(foo: String?) {
    foo == null && throw IllegalArgumentException()
    foo<!UNSAFE_CALL!>.<!>length
}

fun test9(foo: String?) {
    foo == null && return
    foo<!UNSAFE_CALL!>.<!>length
}

fun test10(foo: String?) {
    while (true) {
        foo == null && continue
        foo<!UNSAFE_CALL!>.<!>length
    }
    <!UNREACHABLE_CODE!>foo<!UNSAFE_CALL!>.<!>length<!>
}

fun test11(foo: String?) {
    for (i in 1..3) {
        foo == null && continue
        foo<!UNSAFE_CALL!>.<!>length
    }
}

fun test12(foo: String?) {
    while (true) {
        foo != null && break
        foo<!UNSAFE_CALL!>.<!>length
    }
    foo<!UNSAFE_CALL!>.<!>length
}

fun test13(foo: String?) {
    var i = 0
    while (i == 0) {
        foo != null && break
        foo<!UNSAFE_CALL!>.<!>length
    }
    foo<!UNSAFE_CALL!>.<!>length
}

fun test14(foo: String?) {
    while (true) {
        foo == null && break
        foo<!UNSAFE_CALL!>.<!>length
    }
    foo<!UNSAFE_CALL!>.<!>length
}

fun test15(foo: String?) {
    for (i in 1..3) {
        foo != null && break
    }
    foo<!UNSAFE_CALL!>.<!>length
}

fun test16(foo: String?) {
    for (i in 1..3) {
        foo == null && break
        foo<!UNSAFE_CALL!>.<!>length
    }
    foo<!UNSAFE_CALL!>.<!>length
}

fun test17(foo: Any?) {
    foo !=null && foo is String? || throw IllegalArgumentException()
    foo.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test18(foo: Any?) {
    foo != null && foo !is String && throw IllegalArgumentException()
    var k: String? = <!TYPE_MISMATCH!>foo<!>
}

fun test19(foo: Any?) {
    foo is String || foo == null || throw IllegalArgumentException()
    var k: String? = <!TYPE_MISMATCH!>foo<!>
}

fun test20(foo: Any?) {
    foo == null || foo !is String? && throw IllegalArgumentException()
    var k: String? = <!TYPE_MISMATCH!>foo<!>
}
