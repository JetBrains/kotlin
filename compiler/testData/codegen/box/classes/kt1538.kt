data class Pair<First, Second>(val first: First, val second: Second)

fun parseCatalogs(hashMap: Any?) {
    val r = toHasMap(hashMap)
    if (!r.first) {
        return
    }
    val nodes = r.second
}

fun toHasMap(value: Any?): Pair<Boolean, HashMap<String, Any?>?> {
    if(value is HashMap<*, *>) {
        return Pair(true, value as HashMap<String, Any?>)
    }
    return Pair(false, null as HashMap<String, Any?>?)
}

fun box() : String {
    parseCatalogs(null)
    return "OK"
}
