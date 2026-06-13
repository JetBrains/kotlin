fun main() {
    var local: String? = ""
    if (local != null) {
        run {
            local = null
        }
        println(<expr>local</expr>.length)
    }
}
