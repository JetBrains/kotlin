// FIR_IDENTICAL
class C {
    @Suppress("REDUNDANT_NULLABLE")
    companion object {
        val foo: String?? = null <!USELESS_CAST!>as Nothing??<!>
    }
}