fun box(map: MutableMap<String, String>) {
    map.get<caret>OrDefault("key", "value")
}
