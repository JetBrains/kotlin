package Hello

suspend fun f(o: String, k: String): String {
    return o + k
}

suspend fun main(args: Array<String>) {
    val result = f(args[0], args[1])
    println(result)
}
