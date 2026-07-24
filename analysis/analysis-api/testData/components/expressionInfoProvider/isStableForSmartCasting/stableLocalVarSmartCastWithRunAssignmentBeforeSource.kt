fun main() {
    var local: String? = ""

    run {
        local = null
    }

    if (local != null) {
        println(<expr>local</expr>.length)
    }
}
