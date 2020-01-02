fun main() {
    var list = listOf(1)

    val a: Int? = 2

    a?.let { list += it }
}

operator fun <T> Iterable<T>.plus(element: T): List<T> = null!!
fun <T> listOf(vararg values: T): List<T> = null!!