package one

object ClientTransaction {
    operator fun plusAssign(a: Int) {}
}

fun main() {
    <expr>ClientTransaction += 10</expr>
}
