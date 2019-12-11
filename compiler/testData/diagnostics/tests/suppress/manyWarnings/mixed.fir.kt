@Suppress("REDUNDANT_NULLABLE")
class C {
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    fun foo(): String?? = ""!! as String??
}