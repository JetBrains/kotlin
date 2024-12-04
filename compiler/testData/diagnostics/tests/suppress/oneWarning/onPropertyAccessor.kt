// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class C {
    val foo: String?
        @Suppress("REDUNDANT_NULLABLE")
        get(): String?? = null <!USELESS_CAST!>as Nothing??<!>
}