fun main() {
    var local: C? = C()
    invokeLater {
        local = null
    }
    invokeLater {
        println(<expr>local</expr> != null)
    }
}

fun invokeLater(block: () -> Unit) {}

class C
