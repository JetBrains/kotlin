fun <T : Number> materializeNumber(): T = TODO()

fun a(): Unit = run {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
}

fun b(): Unit = run {
    run {
        <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
    }
}
