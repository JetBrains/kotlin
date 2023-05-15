// ISSUE: KT-58674
// INFERENCE_HELPERS

fun test() {
    while (materialize()) {

    }

    do {

    } while (materialize())

    if (materialize()) {

    }

    when (val it = materialize<Boolean>()) {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>() -> {}
        else -> {}
    }
}
