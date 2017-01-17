// !CHECK_TYPE
// !LANGUAGE: -AdditionalBuiltInsMembers
// SKIP_TXT

class A : java.util.ArrayList<String>() {
    <!UNSUPPORTED_FEATURE!>override<!> fun stream(): java.util.stream.Stream<String> = super.<!DEPRECATION_ERROR!>stream<!>()
}

class A1 : java.util.ArrayList<String>() {
    fun stream(): java.util.stream.Stream<String> = super.<!DEPRECATION_ERROR!>stream<!>()
}

class B : <!DEPRECATION_ERROR!>Throwable<!>("", null, false, false)

fun Throwable.<!EXTENSION_SHADOWED_BY_MEMBER!>fillInStackTrace<!>() = 1

fun foo(x: List<String>, y: Throwable) {
    x.<!DEPRECATION_ERROR!>stream<!>()
    java.util.ArrayList<String>().<!DEPRECATION_ERROR!>stream<!>()

    y.fillInStackTrace() checkType { _<Int>() }

    HashMap<String, Int>().getOrDefault(Any(), null)

    // Falls back to extension in stdlib
    y.printStackTrace()
}
