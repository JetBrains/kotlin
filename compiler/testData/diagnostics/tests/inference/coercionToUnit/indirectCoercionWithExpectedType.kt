// !WITH_NEW_INFERENCE

fun <T> materialize(): T = TODO()

fun a(): Unit = run {
    run {
        // Ok, block is coerced, because it has (indirectly) Unit-expected type
        "hello"
    }
}

fun b(): Unit = run {
    // Ok, expected type is applied
    materialize()
}

fun c(): Unit = run {
    run {
        // Attention!
        // In OI expected type 'Unit' isn't applied here because of implementation quirks (note that OI still applies Unit in case 'e')
        // In NI, it is applied and call is correctly inferred, which is consistent with the previous case
        <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>materialize<!>()
    }
}

fun d(): Unit = run outer@{
    run inner@{
        return@inner <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>materialize<!>()
    }
}

fun e(): Unit = run outer@{
    run inner@{
        return@outer materialize()
    }
}
