// ISSUE: KT-58674
// INFERENCE_HELPERS

fun test() {
    while (<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()) { // K1: OK, K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER

    }

    do {

    } while (<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()) // K1: OK, K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER

    if (materialize()) {

    }

    when (val it = materialize<Boolean>()) {
        materialize() -> {}
        else -> {}
    }
}
