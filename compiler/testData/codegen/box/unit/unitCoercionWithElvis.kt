// WITH_STDLIB
// ISSUE: KT-71751

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0
// ^^^ KT-71751 fixed in 2.1.0-RC

fun launch(x: () -> Unit) {
    x()
}

fun box(): String {
    var result: String = "fail"
    val job = launch {
        "test".let {
            null
        } ?: run { // this is not called if it is the last thing in the block
            result = "OK"
        }
    }

    return result
}
