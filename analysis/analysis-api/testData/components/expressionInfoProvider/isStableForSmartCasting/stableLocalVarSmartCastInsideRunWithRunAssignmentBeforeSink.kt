fun main() {
    var local: String? = ""

    run {
        local = null
    }

    run {
        if (local != null) {
            println(<expr>local</expr>.length)
        }
    }
}
