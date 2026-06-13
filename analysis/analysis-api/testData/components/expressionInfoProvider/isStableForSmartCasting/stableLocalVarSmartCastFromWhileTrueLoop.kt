fun main() {
    var local: String? = null

    while (true) {
        if (local == null) return
        if (condition()) break
    }

    println(<expr>local</expr>.length)
}

fun condition(): Boolean = true
