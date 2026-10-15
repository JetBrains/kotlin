// WITH_STDLIB
fun test(list: List<String>, map: Map<Int, String>) {
    for (element in list) {}
    for (explicit: CharSequence in list) {}
    for ((key, value) in map) {}
    try {
    } catch (e: IllegalStateException) {
    }
}
