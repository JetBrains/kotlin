fun main() {
    var local: C? = C()
    invokeLater {
        println(<expr>local</expr> != null)
    }
    local = C()
}

fun invokeLater(block: () -> Unit) {}

class C
