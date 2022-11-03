// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
class C {
    val foo: String?
        @Suppress("REDUNDANT_NULLABLE")
        get(): String?? = null <!USELESS_CAST!>as Nothing??<!>
}