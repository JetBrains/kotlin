// FIR_DISABLE_LAZY_RESOLVE_CHECKS
@Suppress("REDUNDANT_NULLABLE")
class C {
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    fun foo(): String?? = ""!! as String??
}