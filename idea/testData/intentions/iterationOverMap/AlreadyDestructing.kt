// IS_APPLICABLE: false
// WITH_RUNTIME

fun main(args: Array<String>) {
    val map = hashMapOf(1 to 1)
    for (<caret>(key, value) in map) {
        println(key)
        println(value)
    }
}