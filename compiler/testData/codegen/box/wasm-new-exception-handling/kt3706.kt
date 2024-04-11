// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved
fun f(): Int {
    try {
        return 0
    }
    finally {
        try { // culprit ?? remove this try-catch and it works.
        } catch (ignore: Exception) {
        }
    }
}

fun box(): String {
    if (f() != 0)  return "fail1"

    return "OK"
}