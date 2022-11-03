// FIR_DISABLE_LAZY_RESOLVE_CHECKS
class C {
    fun foo(@Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION") p: String?? = ""!! <!USELESS_CAST!>as String??<!>) = p
}