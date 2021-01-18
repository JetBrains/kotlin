// !API_VERSION: 1.5
// !LANGUAGE: -JvmRecordSupport
// SKIP_TXT
// FILE: JRecord.java
public record JRecord(int x, CharSequence y) {}
// FILE: main.kt

@JvmRecord
class MyRec(
    val x: String,
    val y: Int,
    vararg val z: Double,
)

fun foo(jr: JRecord) {
    <!INAPPLICABLE_CANDIDATE!>JRecord<!>(1, "")

    jr.<!UNRESOLVED_REFERENCE!>x<!>()
    jr.<!UNRESOLVED_REFERENCE!>y<!>()

    jr.<!UNRESOLVED_REFERENCE!>x<!>
    jr.<!UNRESOLVED_REFERENCE!>y<!>
}
