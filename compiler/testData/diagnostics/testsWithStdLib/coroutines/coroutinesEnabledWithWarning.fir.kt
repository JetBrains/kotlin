// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: warn:Coroutines -ReleaseCoroutines

suspend fun suspendHere(): String = "OK"

fun builder(c: suspend () -> Unit) {

}

fun box(): String {
    var result = ""

    builder {
        suspendHere()
    }

    return result
}
