// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +JvmRecordSupport
// FULL_JDK_17

// FILE: MyRecord.java
public record MyRecord(String string, int number) {
    public MyRecord(Long number, String string) {
        this(string, 4);
    }
}

// FILE: main.kt

fun foo(mr: MyRecord) {
    MyRecord("", 1)
    MyRecord(4L, "")
}
