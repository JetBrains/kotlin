// RUN_PIPELINE_TILL: FRONTEND
fun <T : Number> materializeNumber(): T = TODO()

fun a(): Unit = run {
    <!CANNOT_INFER_PARAMETER_TYPE!>materializeNumber<!>()
}

fun b(): Unit = run {
    run {
        <!CANNOT_INFER_PARAMETER_TYPE!>materializeNumber<!>()
    }
}
