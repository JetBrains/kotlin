fun main() {
    var local: C? = C()
    invokeLater {
        println(<expr>local</expr> != null)
    }
    local = null
}

fun invokeLater(block: () -> Unit) {}

class C
