fun main() {
    val local: C? = C()
    println(<expr>local</expr>?.c != null)
}

class C {
    val c: C? = null
}