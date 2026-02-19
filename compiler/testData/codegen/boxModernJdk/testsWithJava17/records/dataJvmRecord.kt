// API_VERSION: 1.5
// LANGUAGE: +JvmRecordSupport
// ENABLE_JVM_PREVIEW

@JvmRecord
data class MyRec<R>(val x: String, val y: R)

fun box(): String {
    val m1 = MyRec("O", "K")
    val m2 = MyRec("O", "K")

    if (m1 != m2) return "fail 1"
    if (m1.hashCode() != m2.hashCode()) return "fail 2"
    if (m1.toString() != "MyRec(x=O, y=K)") return "fail 3: $m1"

    return "OK"
}
