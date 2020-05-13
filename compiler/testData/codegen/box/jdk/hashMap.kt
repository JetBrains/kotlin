// KJS_WITH_FULL_RUNTIME

fun box(): String {
    val map: MutableMap<String, Int> = HashMap<String, Int>()
    map.put("a", 1)
    map.put("bb", 2)
    map.put("ccc", 3)
    map.put("dddd", 4)
    if (map.get("a") != 1) return "fail 1"
    if (map.size != 4) return "fail 2"
    if (map.get("eeeee") != null) return "fail 3"
    if (!map.containsKey("bb")) return "fail 4"
    if (map.keys.contains("ffffff")) return "fail 5"
    return "OK"
}
