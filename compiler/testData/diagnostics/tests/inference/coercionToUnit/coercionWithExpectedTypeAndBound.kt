// !WITH_NEW_INFERENCE

fun <T : Number> materializeNumber(): T = TODO()

fun a(): Unit = run {
    <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
}

fun b(): Unit = run {
    run {
        <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
    }
}