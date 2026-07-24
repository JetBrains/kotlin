fun main() {
    var local: C? = C()
    invokeLater {
        local = C()
    }
    invokeLater {
        println(<expr>local</expr> != null)
    }
}

fun invokeLater(block: () -> Unit) {}

class C
