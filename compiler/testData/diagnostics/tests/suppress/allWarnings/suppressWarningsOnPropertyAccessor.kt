// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
class C {
    val foo: String?
        @Suppress("warnings")
        get(): String?? = null as Nothing?
}