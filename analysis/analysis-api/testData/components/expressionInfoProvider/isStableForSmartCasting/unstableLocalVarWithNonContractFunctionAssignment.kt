fun main() {
    var local: C? = C()

    invokeLater {
        local = null
    }

    println(<expr>local</expr> != null)
}

fun invokeLater(block: () -> Unit) {}

class C {
    val c: C? = null
}
