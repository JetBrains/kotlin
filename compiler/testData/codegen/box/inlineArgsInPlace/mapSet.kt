// IGNORE_BACKEND: WASM
// FULL_JDK
// WITH_RUNTIME

fun box(): String {
    val m = HashMap<String, String>()
    m["ok"] = "OK"
    return m["ok"]!!
}