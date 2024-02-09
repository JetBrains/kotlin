fun <T : Number> materializeNumber(): T = TODO()

fun a(): Unit = run {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
}

fun b(): Unit = run {
    run {
        <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
    }
}
