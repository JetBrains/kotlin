// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: -Coroutines -ReleaseCoroutines

<!UNSUPPORTED_FEATURE!>suspend<!> fun suspendHere(): String = "OK"

fun builder(c: <!UNSUPPORTED_FEATURE!>suspend<!> () -> Unit) {

}

fun box(): String {
    var result = ""

    <!UNSUPPORTED_FEATURE!>builder<!> {
        suspendHere()
    }

    return result
}
