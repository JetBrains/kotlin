fun main() {
    var local: String? = ""

    while (true) {
        if (local == null) return
        run {
            local = ""
        }
        if (condition()) break
    }

    println(<expr>local</expr>.length)
}

fun condition(): Boolean = true
