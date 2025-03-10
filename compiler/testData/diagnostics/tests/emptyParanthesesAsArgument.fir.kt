// RUN_PIPELINE_TILL: FRONTEND
fun f(a: Int, b: Int, c: Int) {}

fun main() {
    f(c = 3, (), a = 1)
}