// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// FILE: MyRecord.java
public record MyRecord(int x, CharSequence y) {

}

// FILE: main.kt

fun foo(mr: MyRecord) {
    <!INAPPLICABLE_CANDIDATE!>MyRecord<!>(1, "")

    mr.<!UNRESOLVED_REFERENCE!>x<!>()
    mr.<!UNRESOLVED_REFERENCE!>y<!>()

    mr.<!UNRESOLVED_REFERENCE!>x<!>
    mr.<!UNRESOLVED_REFERENCE!>y<!>
}
