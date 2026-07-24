fun main() {
    var local: String? = ""
    if (local != null) {
        invokeLater {
            local = "value"
        }
        println(<expr>local</expr>.length)
    }
}

fun invokeLater(block: () -> Unit) {}
