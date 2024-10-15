// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL

fun <T: Any> dereferenceClass(): Any =
        <!TYPE_PARAMETER_AS_REIFIED!>T::class<!>
