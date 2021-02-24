// !WITH_NEW_INFERENCE
package bar


// should be thrown away

fun <R> List<R>.a() {}

fun test1(i: Int?) {
    1.<!INAPPLICABLE_CANDIDATE!>a<!>()
    i<!UNSAFE_CALL!>.<!>a()
}

fun <R> test2(c: Collection<R>) {
    c.<!INAPPLICABLE_CANDIDATE!>a<!>()
}

fun Int.foo() {}

fun test3(s: String?) {
    "".<!INAPPLICABLE_CANDIDATE!>foo<!>()
    s<!UNSAFE_CALL!>.<!>foo()
    "".<!INAPPLICABLE_CANDIDATE!>foo<!>(1)
    s.<!INAPPLICABLE_CANDIDATE!>foo<!>("a")
}

interface A
fun <T: A> T.c() {}

fun test4() {
    1.<!INAPPLICABLE_CANDIDATE!>c<!>()
}


// should be an error on receiver, shouldn't be thrown away

fun test5() {
    1.<!UNRESOLVED_REFERENCE!>(fun String.()=1)<!>()
}

fun <R: Any> R?.sure() : R = this!!

fun <T> test6(l: List<T>?) {
    l.<!INAPPLICABLE_CANDIDATE!>sure<!><T>()
}


fun List<String>.b() {}

fun test7(l: List<String?>) {
    l.<!INAPPLICABLE_CANDIDATE!>b<!>()
}

fun test8(l: List<Any>?) {
    l<!UNSAFE_CALL!>.<!>b()
}
