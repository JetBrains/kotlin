fun main() {
    var local: String? = ""
    if (local != null) {
        run {
            local = "value"
        }
        println(<expr>local</expr>.length)
    }
}
