// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
@Suppress("REDUNDANT_NULLABLE")
object C {
    fun foo(): String?? = null <!USELESS_CAST!>as Nothing??<!>
}