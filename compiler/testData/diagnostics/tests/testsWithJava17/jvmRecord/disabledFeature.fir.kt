// !API_VERSION: 1.5
// !LANGUAGE: -JvmRecordSupport
// SKIP_TXT
// FILE: JRecord.java
public record JRecord(int x, CharSequence y) {}
// FILE: main.kt

<!UNSUPPORTED_FEATURE!>@JvmRecord<!>
class MyRec(
    val x: String,
    val y: Int,
    vararg val z: Double,
)

fun foo(jr: JRecord) {
    JRecord(1, "")

    jr.x()
    jr.y()

    jr.x
    jr.y
}
