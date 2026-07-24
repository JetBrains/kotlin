fun main() {
    var local: String? = ""

    invokeLater {
        local = null
    }

    invokeLater {
        if (local != null) {
            println(<expr>local</expr>.length)
        }
    }
}

fun invokeLater(block: () -> Unit) {}
