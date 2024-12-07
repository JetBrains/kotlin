// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTION_INHERITANCE
// KJS_WITH_FULL_RUNTIME
// DONT_TARGET_EXACT_BACKEND: NATIVE

open class Map1 : HashMap<String, Any?>()
class Map2 : Map1()
fun box(): String {
    val m = Map2()
    if (m.entries.size != 0) return "fail 1"

    m.put("56", "OK")
    val x = m.entries.iterator().next()

    if (x.key != "56" || x.value != "OK") return "fail 2"

    return "OK"
}
