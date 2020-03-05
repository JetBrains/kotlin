// !LANGUAGE: +NewInference

val Int.plusAssign: (Int) -> Unit
    get() = {}

fun main() {
    1 += 2
}
