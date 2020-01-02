// !WITH_NEW_INFERENCE

fun <T : Number> materializeNumber(): T = TODO()

fun a(): Unit = run {
    materializeNumber()
}

fun b(): Unit = run {
    run {
        materializeNumber()
    }
}