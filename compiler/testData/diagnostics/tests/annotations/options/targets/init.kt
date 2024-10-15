// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
annotation class base

@base class My {
    <!WRONG_ANNOTATION_TARGET!>@base<!> init {
    }
}
