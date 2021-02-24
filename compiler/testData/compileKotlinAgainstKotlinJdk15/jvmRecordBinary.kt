// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// JVM_TARGET: 15
// ENABLE_JVM_PREVIEW
// FILE: A.kt
@JvmRecord
data class MyRecord(val foo: String, val bar: String)

// FILE: B.kt

fun main() {
    val myRecord = MyRecord("O", "K")
    val s = myRecord.foo + myRecord.bar
    if (s != "OK") {
        throw AssertionError("fail: $s")
    }
}
