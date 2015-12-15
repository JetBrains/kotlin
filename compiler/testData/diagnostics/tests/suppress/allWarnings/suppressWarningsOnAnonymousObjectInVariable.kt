@Suppress("warnings")
val anonymous = object {
    fun foo(p: String?? = "" as String) {}
}