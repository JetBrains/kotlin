fun main() {
    val local: C? = C()
    println(<expr>local?.c</expr> != null)
}

class C {
    var c: C? = null
}