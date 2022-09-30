// FIR_IDENTICAL
// FIR_IDE_IGNORE
// API_VERSION: 1.5
// LANGUAGE: +JvmRecordSupport
// SCOPE_DUMP: MyRecord:x

// FILE: MyRecord.java
public record MyRecord(String x) {
    public String x() {
        System.out.println("hello");
        return x;
    }
}

// FILE: main.kt

fun takeString(s: String) {}

fun foo(mr: MyRecord) {
    takeString(mr.x)
    takeString(mr.x())
}
