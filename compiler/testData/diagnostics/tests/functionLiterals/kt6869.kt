fun main(args : Array<String>) {
    var list = listOf(1)

    val a: Int? = 2

    a?.let { list += it }
}

operator fun <T> Iterable<T>.plus(<!UNUSED_PARAMETER!>element<!>: T): List<T> = null!!
fun <T> listOf(vararg <!UNUSED_PARAMETER!>values<!>: T): List<T> = null!!