// FIR_IDENTICAL
// !LANGUAGE: +VariableDeclarationInWhenSubject
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

fun foo(s1: Int, s2: Int) = s1 + s2

fun test1(x: String?) =
    when (val y = x?.length) {
        null -> 0
        else -> foo(x.length, y)
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
        "foo" -> x.length
        "bar" -> y.length
    }
}

class Inv<T>(val data: T)

fun test5(x: Inv<out Any?>) {
    when (val y = x.data) {
        is String -> y.length // should be ok
        null -> x.data.<!UNRESOLVED_REFERENCE!>length<!> // should be error
    }
}

fun test6(x: Inv<out String?>) {
    when (val y = x.data) {
        is String -> x.data.length // should be ok
    }
}
