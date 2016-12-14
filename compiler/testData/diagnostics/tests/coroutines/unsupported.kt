// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: -Coroutines

<!UNSUPPORTED_FEATURE!>suspend<!> fun suspendHere(): String = "OK"

fun builder(c: suspend () -> Unit) {

}

fun box(): String {
    var result = ""

    <!UNSUPPORTED_FEATURE!>builder<!> {
        suspendHere()
    }

    return result
}
