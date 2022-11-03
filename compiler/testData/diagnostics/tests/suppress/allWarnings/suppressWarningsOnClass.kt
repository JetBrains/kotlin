// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
@Suppress("warnings")
class C {
    fun foo(p: String??) {
        // Make sure errors are not suppressed:
        <!VAL_REASSIGNMENT!>p<!> = ""
    }
}