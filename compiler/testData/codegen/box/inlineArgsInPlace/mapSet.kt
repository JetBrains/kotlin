// FULL_JDK
// WITH_STDLIB

fun box(): String {
    val m = HashMap<String, String>()
    m["ok"] = "OK"
    return m["ok"]!!
}