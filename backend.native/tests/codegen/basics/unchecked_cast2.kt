fun main(args: Array<String>) {
    try {
        val x = cast<String>(Any())
        println(x.length)
    } catch (e: Throwable) {
        println("Ok")
    }
}

fun <T> cast(x: Any?) = x as T