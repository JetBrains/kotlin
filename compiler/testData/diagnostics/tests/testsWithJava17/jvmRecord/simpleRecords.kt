// FIR_IDENTICAL
// API_VERSION: 1.5
// LANGUAGE: +JvmRecordSupport
// SCOPE_DUMP: MyRecord:x;y;z

// FILE: MyRecord.java
public record MyRecord(CharSequence x, int y, String... z) {}

// FILE: main.kt

fun takeInt(x: Int) {}
fun takeCharSequence(s: CharSequence) {}
fun takeStringArray(a: Array<out String>) {}

fun foo(mr: MyRecord) {
    MyRecord("", 1, "a", "b")

    takeCharSequence(mr.x())
    takeInt(mr.y())
    takeStringArray(mr.z())

    takeCharSequence(mr.x)
    takeInt(mr.y)
    takeStringArray(mr.z)
}
