fun main() {
    var local: String? = null

    do {
        if (local == null) return
    } while (condition())

    println(<expr>local</expr>.length)
}

fun condition(): Boolean = true
