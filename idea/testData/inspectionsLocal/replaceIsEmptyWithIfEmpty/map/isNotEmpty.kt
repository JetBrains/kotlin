// WITH_RUNTIME
fun test(map: Map<Int, Int>): Map<Int, Int> {
    return if (map.isNotEmpty<caret>()) {
        map
    } else {
        mapOf(1 to 2)
    }
}