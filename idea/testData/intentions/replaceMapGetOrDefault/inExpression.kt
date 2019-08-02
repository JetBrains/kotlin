// RUNTIME_WITH_FULL_JDK
fun test(map: Map<Int, String>) {
    map.<caret>getOrDefault(1, "bar") + "baz"
}
