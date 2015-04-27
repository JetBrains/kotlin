fun listOf<T>(): List<T> = null!!
fun listOf<T>(vararg <!UNUSED_PARAMETER!>values<!>: T): List<T> = null!!

val flag = true

val a: () -> List<Int> = l@ {
    if (flag) return@l listOf()
    listOf(5)
}