// SKIP_TXT
// ISSUE: KT-57513, KT-58169

fun string(foo: Any) {
    val string = ""
    if ("" == foo) <!DEBUG_INFO_SMARTCAST!>foo<!>.length
    if (string == foo) <!DEBUG_INFO_SMARTCAST!>foo<!>.length
    if (foo == "") foo.<!UNRESOLVED_REFERENCE!>length<!>
}

fun int(foo: Any) {
    val int = 1
    if (1 == foo) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1)
    if (int == foo) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1)
    if (foo == 1) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1)
}

fun long(foo: Any) {
    val long = 1L
    if (1L == foo) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1L)
    if (long == foo) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1L)
    if (foo == 1L) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1L)
}

fun char(foo: Any) {
    val char = 'a'
    if ('a' == foo) foo.<!UNRESOLVED_REFERENCE!>compareTo<!>('a')
    if (char == foo) foo.<!UNRESOLVED_REFERENCE!>compareTo<!>('a')
    if (foo == 'a') foo.<!UNRESOLVED_REFERENCE!>compareTo<!>('a')
}

class A { fun a() = Unit }
class B { fun b() = Unit; override fun equals(other: Any?): Boolean = this === other }
data class C(val x: Int) { fun c() = Unit }
open class D { fun d() = Unit }
enum class E { ONE; fun e() = Unit }

fun testA(foo: A, bar: Any) {
    if (foo == bar) <!DEBUG_INFO_SMARTCAST!>bar<!>.a()
    if (bar == foo) bar.<!UNRESOLVED_REFERENCE!>a<!>()
}

fun testNullableA(foo: A?, bar: Any?) {
    if (foo != null && foo == bar) <!DEBUG_INFO_SMARTCAST!>bar<!>.a()
}

fun testB(foo: B, bar: Any) {
    if (foo == bar) bar.<!UNRESOLVED_REFERENCE!>b<!>()
    if (bar == foo) bar.<!UNRESOLVED_REFERENCE!>b<!>()
}

fun testNullableB(foo: B?, bar: B?) {
    if (foo != null && foo == bar) <!DEBUG_INFO_SMARTCAST!>bar<!>.b()
}

fun testC(foo: C, bar: Any) {
    if (foo == bar) bar.<!UNRESOLVED_REFERENCE!>c<!>()
    if (bar == foo) bar.<!UNRESOLVED_REFERENCE!>c<!>()
}

fun testNullableC(foo: C?, bar: C?) {
    if (foo != null && foo == bar) <!DEBUG_INFO_SMARTCAST!>bar<!>.c()
}

fun testD(foo: D, bar: Any) {
    if (foo == bar) bar.<!UNRESOLVED_REFERENCE!>d<!>()
    if (bar == foo) bar.<!UNRESOLVED_REFERENCE!>d<!>()
}

fun testNullableD(foo: D?, bar: D?) {
    if (foo != null && foo == bar) <!DEBUG_INFO_SMARTCAST!>bar<!>.d()
}

fun testE(foo: E, bar: Any) {
    if (foo == bar) bar.<!UNRESOLVED_REFERENCE!>e<!>()
    if (bar == foo) bar.<!UNRESOLVED_REFERENCE!>e<!>()
}

fun testNullableE(foo: E?, bar: E?) {
    if (foo != null && foo == bar) <!DEBUG_INFO_SMARTCAST!>bar<!>.e()
}

fun testSmartcast(foo: Any, bar: Any) {
    if (foo is A && foo == bar) {
        bar.<!UNRESOLVED_REFERENCE!>a<!>()
    }
    if (foo is B && foo == bar) {
        bar.<!UNRESOLVED_REFERENCE!>b<!>()
    }
    if (foo is C && foo == bar) {
        bar.<!UNRESOLVED_REFERENCE!>c<!>()
    }
    if (foo is D && foo == bar) {
        bar.<!UNRESOLVED_REFERENCE!>d<!>()
    }
    if (foo is E && foo == bar) {
        bar.<!UNRESOLVED_REFERENCE!>e<!>()
    }
}
