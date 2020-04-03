// !WITH_NEW_INFERENCE
package bar


// should be thrown away

fun <R> List<R>.a() {}

fun test1(i: Int?) {
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>a<!>()
    i.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>a<!>()
}

fun <R> test2(c: Collection<R>) {
    c.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>a<!>()
}

fun Int.foo() {}

fun test3(s: String?) {
    "".<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
    s.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
    "".<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>(1)
    s.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>("a")
}

interface A
fun <T: A> T.c() {}

fun test4() {
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>c<!>()
}


// should be an error on receiver, shouldn't be thrown away

fun test5() {
    <!OI;TYPE_MISMATCH!>1<!>.<!NI;FUNCTION_EXPECTED!>(fun String.()=1)<!>()
}

fun <R: Any> R?.sure() : R = this!!

fun <T> test6(l: List<T>?) {
    <!OI;TYPE_MISMATCH!>l<!>.<!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>sure<!><<!OI;UPPER_BOUND_VIOLATED!>T<!>>()
}


fun List<String>.b() {}

fun test7(l: List<String?>) {
    <!OI;TYPE_MISMATCH!>l<!>.<!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>b<!>()
}

fun test8(l: List<Any>?) {
    <!OI;TYPE_MISMATCH!>l<!>.<!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>b<!>()
}