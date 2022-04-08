// FIR_IDE_IGNORE
// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// FILE: MyRecord.java
public record MyRecord(int x, CharSequence y) {

}

// FILE: main.kt

fun foo(mr: MyRecord) {
    MyRecord(<!TOO_MANY_ARGUMENTS!>1<!>, <!TOO_MANY_ARGUMENTS!>""<!>)

    mr.<!UNRESOLVED_REFERENCE!>x<!>()
    mr.<!UNRESOLVED_REFERENCE!>y<!>()

    mr.<!UNRESOLVED_REFERENCE!>x<!>
    mr.<!UNRESOLVED_REFERENCE!>y<!>
}
