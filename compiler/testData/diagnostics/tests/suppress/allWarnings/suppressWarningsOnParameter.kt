// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
class C {
    fun foo(@Suppress("warnings") p: String?? = "" as String) {}
}