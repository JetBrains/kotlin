fun Int.addOne(): Int {
    return this + 1
}

val Int.repeat: Int
    get() = this

fun main() {
    val i = 2
    i.addOne()
    val p = i.repeat * 2
}