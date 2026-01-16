fun main() {
    val local: C? = C()
    println(<expr>local?.c</expr> != null)
}

open class C {
    open val c: C? = null
}