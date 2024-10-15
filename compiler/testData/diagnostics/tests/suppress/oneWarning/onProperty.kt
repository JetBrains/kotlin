// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class C {
    @Suppress("REDUNDANT_NULLABLE")
    val foo: String?? = null <!USELESS_CAST!>as Nothing?<!>
}