// ISSUE: KT-24779
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

class C<T>(val data: T)

fun test1(x: C<out Any?>) {
    val y = x.data
    require(y is String)
    x.data.<!UNRESOLVED_REFERENCE!>length<!>
    y.length
}

fun test2(x: C<out String?>) {
    val y = x.data
    require(y is String)
    x.data.length
    y.length
}

fun test3(x: C<out Int?>) {
    val y = x.data
    require(y is Number)
    x.data.inc()
    y.inc()
}

fun test4(x: C<in Any?>) {
    val y = x.data
    require(y is String)
    x.data.<!UNRESOLVED_REFERENCE!>length<!>
    y.length
}

fun test5(x: C<in String?>) {
    val y = x.data
    require(y is String)
    x.data.<!UNRESOLVED_REFERENCE!>length<!>
    y.length
}

fun test6(x: C<in Number?>) {
    val y = x.data
    require(y is Int)
    x.data.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    y.inc()
}
