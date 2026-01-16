fun main() {
    val local: C? = C()
    println(<expr>local</expr> != null)
}

class C {
    val c: C? = null
}