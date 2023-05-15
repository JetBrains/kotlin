// ISSUE: KT-58674
// INFERENCE_HELPERS

fun test() {
    while (materialize()) { // K1: OK, K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER

    }

    do {

    } while (materialize()) // K1: OK, K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER

    if (materialize()) {

    }

    when (val it = materialize<Boolean>()) {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>() -> {}
        else -> {}
    }
}
