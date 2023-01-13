fun main(args: Array<String>) {
    val anyClass = Any()
    funOne(<expr>anyClass</expr>.class)
}

fun funOne(x: Any): Unit {}