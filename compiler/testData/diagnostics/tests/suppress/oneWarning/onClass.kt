// FIR_IDENTICAL
@Suppress("REDUNDANT_NULLABLE")
class C {
    fun foo(): String?? = null <!USELESS_CAST!>as Nothing??<!>
}