// !WITH_NEW_INFERENCE

fun <T : Number> materializeNumber(): T = TODO()

fun a(): Unit = run {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>materializeNumber<!>()
}

fun b(): Unit = run {
    run {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>materializeNumber<!>()
    }
}
