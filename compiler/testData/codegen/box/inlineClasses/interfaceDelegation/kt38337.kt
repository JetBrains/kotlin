// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

inline class Wrapper(val id: Int)

class DMap(private val map: Map<Wrapper, String>) :
        Map<Wrapper, String> by map

fun box(): String {
    val dmap = DMap(mutableMapOf(Wrapper(42) to "OK"))
    return dmap[Wrapper(42)] ?: "Fail"
}