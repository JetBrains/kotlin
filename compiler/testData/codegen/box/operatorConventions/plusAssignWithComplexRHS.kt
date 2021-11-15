// WITH_STDLIB

var map: Map<Any, Set<Any>> = emptyMap()

fun box(): String {
    map += "OK" to emptySet()
    return map.keys.first().toString()
}
