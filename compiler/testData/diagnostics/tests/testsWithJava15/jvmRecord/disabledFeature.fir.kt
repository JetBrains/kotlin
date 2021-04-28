// FIR_IDE_IGNORE
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
    JRecord(<!TOO_MANY_ARGUMENTS!>1<!>, <!TOO_MANY_ARGUMENTS!>""<!>)

    jr.<!UNRESOLVED_REFERENCE!>x<!>()
    jr.<!UNRESOLVED_REFERENCE!>y<!>()

    jr.<!UNRESOLVED_REFERENCE!>x<!>
    jr.<!UNRESOLVED_REFERENCE!>y<!>
}
