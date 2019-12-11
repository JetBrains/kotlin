@Deprecated("Object")
object Obsolete {
    fun use() {}
}

fun useObject() {
    Obsolete.use()
    val x = Obsolete
}
