// IS_APPLICABLE: false
// WITH_RUNTIME

fun main(args: Array<String>) {
    val map = hashMapOf(1 to 1)
    for (entry <caret>in map) {
        val key = entry.key
        val value = entry.value

        println(key)
        println(value)
    }
}