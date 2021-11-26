// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_STDLIB

enum class E {
    VALUE,
    VALUE2
}

class C(val nums: Map<E, Int>) {
    val normalizedNums = loadNormalizedNums()

    private fun loadNormalizedNums(): Map<E, Float> {
        val vals = nums.values
        val min = vals.minOrNull()!!
        val max = vals.maxOrNull()!!
        val rangeDiff = (max - min).toFloat()
        val normalizedNums = nums.map { kvp ->
            val (e, num) = kvp
            //val e = kvp.key
            //val num = kvp.value
            val normalized = (num - min) / rangeDiff
            Pair(e, normalized)
        }.toMap()
        return normalizedNums
    }
}

fun box(): String {
    val res = C(hashMapOf(E.VALUE to 11, E.VALUE2 to 12)).normalizedNums.values.sorted().joinToString()
    return  if ("0.0, 1.0" == res) "OK" else "fail $res"
}
