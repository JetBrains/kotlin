// WITH_RUNTIME

fun main(args: Array<String>) {
    val map = hashMapOf(1 to 1)
    val entries = map.entries
    for (<caret>entry in entries) {
        val key = entry.key
        val value = entry.value

        println(key)
        println(value)
    }
}