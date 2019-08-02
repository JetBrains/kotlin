// RUNTIME_WITH_FULL_JDK
fun test(map: Map<Int, String>) {
    map.getOrDefault<caret>(1, "bar")
}