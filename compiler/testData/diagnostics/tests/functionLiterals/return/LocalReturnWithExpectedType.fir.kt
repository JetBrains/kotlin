fun <T> listOf(): List<T> = null!!
fun <T> listOf(vararg values: T): List<T> = null!!

val flag = true

val a: () -> List<Int> = l@ {
    if (flag) return@l listOf()
    listOf(5)
}