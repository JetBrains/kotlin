// FIR_IDENTICAL
class C {
    fun foo(@Suppress("REDUNDANT_NULLABLE") p: String?? = null <!USELESS_CAST!>as Nothing??<!>) = p
}