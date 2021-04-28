// !WITH_NEW_INFERENCE

fun <T : Number> materializeNumber(): T = TODO()

fun a(): Unit = run {
    <!NEW_INFERENCE_ERROR!>materializeNumber()<!>
}

fun b(): Unit = run {
    run {
        <!NEW_INFERENCE_ERROR!>materializeNumber()<!>
    }
}
