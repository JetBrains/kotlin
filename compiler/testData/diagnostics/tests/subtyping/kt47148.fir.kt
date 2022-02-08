// WITH_STDLIB

interface Visitor<T> {
    fun visit(key: String): T
}
val str1: String? = null
val str2: String? = null
fun <T> visit(arg: Visitor<T>): T = str1?.let { return@visit arg.visit(it) }
    ?: str2?.let { return@visit arg.visit(it) }
    ?: error("error")