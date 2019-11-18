// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

enum class E {
    VALUE,
    VALUE2
}

class C(val nums: Map<E, Int>) {
    val normalizedNums = loadNormalizedNums()

    private fun loadNormalizedNums(): Map<E, Float> {
        val vals = nums.values
        val min = vals.min()!!
        val max = vals.max()!!
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
