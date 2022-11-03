// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
class C {
    @Suppress("warnings")
    companion object {
        val foo: String?? = null as Nothing?
    }
}