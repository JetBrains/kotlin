// !LANGUAGE: -AdditionalBuiltInsMembers
// SKIP_TXT

class A : java.util.ArrayList<String>() {
    <!UNSUPPORTED_FEATURE!>override<!> fun stream(): java.util.stream.Stream<String> = super.<!UNSUPPORTED_FEATURE!>stream<!>()
}

class A1 : java.util.ArrayList<String>() {
    // TODO: should be allowed
    <!VIRTUAL_MEMBER_HIDDEN!>fun stream(): java.util.stream.Stream<String><!> = super.<!UNSUPPORTED_FEATURE!>stream<!>()
}

class B : <!UNSUPPORTED_FEATURE!>Throwable<!>("", null, false, false)

fun foo(x: List<String>) {
    x.<!UNSUPPORTED_FEATURE!>stream<!>()
    java.util.ArrayList<String>().<!UNSUPPORTED_FEATURE!>stream<!>()
}
