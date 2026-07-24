fun main() {
    var local: String? = ""
    if (local != null) {
        invokeLater {
            local = null
        }
        println(<expr>local</expr>.length)
    }
}

fun invokeLater(block: () -> Unit) {}
