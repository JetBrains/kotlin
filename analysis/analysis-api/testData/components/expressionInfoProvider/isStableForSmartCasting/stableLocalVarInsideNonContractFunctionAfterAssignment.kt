fun main() {
    var local: C? = null
    local = C()
    invokeLater {
        println(<expr>local</expr> != null)
    }
}

fun invokeLater(block: () -> Unit) {}

class C
