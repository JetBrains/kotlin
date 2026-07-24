fun main() {
    var local: String? = ""

    invokeLater {
        local = null
    }

    if (local != null) {
        println(<expr>local</expr>.length)
    }
}

fun invokeLater(block: () -> Unit) {}
