// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// FILE: MyRecord.java
public record MyRecord(int x, CharSequence y) {

}

// FILE: main.kt

fun foo(mr: MyRecord) {
    MyRecord(1, "")

    mr.x()
    mr.y()

    mr.x
    mr.y
}
