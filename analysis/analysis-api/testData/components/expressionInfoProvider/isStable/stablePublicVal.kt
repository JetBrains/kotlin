fun main() {
    val local: C? = C()
    println(<expr>local?.c</expr> != null)
}

class C {
    val c: C? = null
}