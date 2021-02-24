// FIR_IDENTICAL
fun get(map: Map<String, Int>, key: String?): Int? {
    return map[key]?.let { x ->
        return x
    }
}
