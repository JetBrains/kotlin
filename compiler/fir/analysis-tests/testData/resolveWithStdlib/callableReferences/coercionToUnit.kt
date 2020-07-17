public fun <T> T.myAlso(block: (T) -> Unit): T = TODO()

class B {
    fun add(x: String): Boolean = true
}

fun main(b: B) {
    "".myAlso(b::add)
}
