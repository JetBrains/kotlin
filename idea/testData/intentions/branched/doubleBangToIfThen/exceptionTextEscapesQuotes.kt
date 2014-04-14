// WITH_RUNTIME
val map = mapOf("one" to 1, "two" to null, "three" to 3)
fun main(args: Array<String>) {
    map.get("one")<caret>!!
}
