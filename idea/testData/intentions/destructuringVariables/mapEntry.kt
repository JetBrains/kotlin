// WITH_RUNTIME
fun foo(): Map.Entry<Int, Int> = mapOf(1 to 2).entries.first()

fun bar(): Int {
    val <caret>v = foo()
    return v.key + v.value
}
