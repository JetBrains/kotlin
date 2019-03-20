// !WITH_NEW_INFERENCE

fun <T : Number> materializeNumber(): T = TODO()

fun a(): Unit = run {
    <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
}

fun b(): Unit = run {
    run {
        <!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
    }
}