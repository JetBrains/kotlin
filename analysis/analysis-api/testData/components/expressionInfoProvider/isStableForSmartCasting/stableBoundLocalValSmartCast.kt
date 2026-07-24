fun main() {
    val a: Any? = 42
    val b = a

    if (b is Int) {
        println(<expr>a</expr>.inc())
    }
}
