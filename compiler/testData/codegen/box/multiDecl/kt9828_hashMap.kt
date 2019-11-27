// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val hashMap = HashMap<String, Int>()
    hashMap.put("one", 1)
    hashMap.put("two", 2)
    for ((key, value) in hashMap) {
    }

    return "OK"
}
