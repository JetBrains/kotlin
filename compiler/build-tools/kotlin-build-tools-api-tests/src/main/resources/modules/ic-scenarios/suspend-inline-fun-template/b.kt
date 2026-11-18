suspend inline fun g(): Int = f()

suspend fun main() {
    println(g())
}
