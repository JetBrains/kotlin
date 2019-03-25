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
    <!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>materialize<!>()
}

fun c(): Unit = run {
    run {
        // Attention!
        // In OI expected type 'Unit' isn't applied here because of implementation quirks (note that OI still applies Unit in case 'e')
        // In NI, it is applied and call is correctly inferred, which is consistent with the previous case
        <!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
    }
}

fun d(): Unit = run outer@{
    run inner@{
        <!NI;UNREACHABLE_CODE!>return@inner<!> <!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
    }
}

fun e(): Unit = run outer@{
    run inner@{
        return@outer materialize()
    }
}