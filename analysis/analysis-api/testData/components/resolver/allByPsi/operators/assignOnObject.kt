package one

object ClientTransaction {
    operator fun plusAssign(a: Int) {}
    operator fun minusAssign(b: String) {}
}

fun main() {
    ClientTransaction += 10
    ClientTransaction -= "foo"
}
