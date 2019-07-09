// !LANGUAGE: +VariableDeclarationInWhenSubject
// !WITH_NEW_INFERENCE

fun foo(s1: Int, s2: Int) = s1 + s2

fun test1(x: String?) =
    when (val y = x?.length) {
        null -> 0
        else -> foo(<!DEBUG_INFO_SMARTCAST!>x<!>.length, <!DEBUG_INFO_SMARTCAST!>y<!>)
    }

fun test2(x: String?) {
    when (val y = run { x!! }) {
        "foo" -> x<!UNSAFE_CALL!>.<!>length
        "bar" -> y.length
    }
}

fun test3(x: String?, y: String?) {
    when (val z = x ?: y!!) {
        "foo" -> x<!UNSAFE_CALL!>.<!>length
        "bar" -> y<!UNSAFE_CALL!>.<!>length
        "baz" -> z.length
    }
}

fun <T> id(x: T): T = x

fun test4(x: String?) {
    when (val y = id(x!!)) {
        "foo" -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        "bar" -> y.length
    }
}

class Inv<T>(val data: T)

fun test5(x: Inv<out Any?>) {
    when (val y = x.data) {
        is String -> <!DEBUG_INFO_SMARTCAST!>y<!>.length // should be ok
        null -> x.data.<!UNRESOLVED_REFERENCE!>length<!> // should be error
    }
}

fun test6(x: Inv<out String?>) {
    when (val <!UNUSED_VARIABLE!>y<!> = x.data) {
        is String -> <!OI;DEBUG_INFO_SMARTCAST!>x.data<!><!NI;UNSAFE_CALL!>.<!>length // should be ok
    }
}