// FIR_IDENTICAL
class C {
    fun foo(): Any? {
        @Suppress("REDUNDANT_NULLABLE")
        val v: String?? = null <!USELESS_CAST!>as Nothing??<!>
        return v
    }
}