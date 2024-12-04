// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun foo(): Any? {
    @Suppress("REDUNDANT_NULLABLE")
    return null <!USELESS_CAST!>as Nothing??<!>
}