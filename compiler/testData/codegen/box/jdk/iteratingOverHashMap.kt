// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME

fun box() : String {
    if (!testIteratingOverMap1()) return "fail 1"
    if (!testIteratingOverMap2()) return "fail 2"
    return "OK"
}

fun testIteratingOverMap1() : Boolean {
    val map = HashMap<String, Int>()
    map.put("a", 1)
    for (entry in map.entries) {
        entry.setValue(2)
    }
    return map.get("a") == 2
}

fun testIteratingOverMap2() : Boolean {
    val map : MutableMap<String, Int> = HashMap<String, Int>()
    map.put("a", 1)
    for (entry in map.entries) {
        entry.setValue(2)
    }
    return map.get("a") == 2
}