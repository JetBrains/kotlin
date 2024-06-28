// FIR_IDENTICAL
// LANGUAGE: +JvmRecordSupport

// FILE: MyRecord.java
public record MyRecord(String string, int number) {
    public MyRecord(Long number, String string) {
        this(string, 4)
    }
}

// FILE: main.kt

fun foo(mr: MyRecord) {
    MyRecord("", 1)
    MyRecord(4L, "")
}
