// WITH_STDLIB

fun box(map: MutableMap<String, String>) {
    map.get<caret>OrDefault("key", null)
}
