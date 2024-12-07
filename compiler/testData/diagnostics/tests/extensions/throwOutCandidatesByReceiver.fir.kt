// RUN_PIPELINE_TILL: FRONTEND
package bar


// should be thrown away

fun <R> List<R>.a() {}

fun test1(i: Int?) {
    1.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>a<!>()
    i.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>a<!>()
}

fun <R> test2(c: Collection<R>) {
    c.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>a<!>()
}

fun Int.foo() {}

fun test3(s: String?) {
    "".<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
    s.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
    "".foo(<!TOO_MANY_ARGUMENTS!>1<!>)
    s.foo(<!TOO_MANY_ARGUMENTS!>"a"<!>)
}

interface A
fun <T: A> T.c() {}

fun test4() {
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>c<!>()
}


// should be an error on receiver, shouldn't be thrown away

fun test5() {
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(fun String.()=1)<!>()
}

fun <R: Any> R?.sure() : R = this!!

fun <T> test6(l: List<T>?) {
    l.<!INAPPLICABLE_CANDIDATE!>sure<!><<!UPPER_BOUND_VIOLATED!>T<!>>()
}


fun List<String>.b() {}

fun test7(l: List<String?>) {
    l.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>b<!>()
}

fun test8(l: List<Any>?) {
    l.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>b<!>()
}
