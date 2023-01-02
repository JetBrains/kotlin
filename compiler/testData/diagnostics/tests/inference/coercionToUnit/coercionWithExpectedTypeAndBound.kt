// FIR_IDENTICAL
fun <T : Number> materializeNumber(): T = TODO()

fun a(): Unit = run {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
}

fun b(): Unit = run {
    run {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeNumber<!>()
    }
}
