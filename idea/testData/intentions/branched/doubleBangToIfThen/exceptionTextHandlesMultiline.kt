// WITH_RUNTIME
fun main(args: Array<String>) {
    val t = mapOf("one" to 1,
                  "two" to null,
                  "three" to 3).get("one")<caret>!!
}
