// !LANGUAGE: +JvmRecordSupport
// IGNORE_BACKEND_FIR: JVM_IR
// ENABLE_JVM_PREVIEW
// FILE: MyRec.java
public record MyRec(String name) {}

// FILE: recordPropertyAccess.kt
fun box(): String {
    val r = MyRec("OK")
    if (r.name() != "OK") return "fail 1"

    return r.name
}
