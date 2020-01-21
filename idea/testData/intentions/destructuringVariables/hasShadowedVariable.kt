// WITH_RUNTIME
fun main() {
    data class A(var x: Int)

    val <caret>a = A(0)
    val x = a.x

    run {
        val x = 1
        val z = a.x
    }
}